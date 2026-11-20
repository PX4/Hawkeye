#include "terrain_dem.h"

#include <float.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "raymath.h"

#define EARTH_RADIUS 6371000.0
#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

#ifdef HAWKEYE_GEOTIFF
#include <tiffio.h>

#define TAG_PIXEL_SCALE 33550
#define TAG_TIEPOINT    33922
#define TAG_GEOKEYS     34735
#define TAG_GDAL_NODATA 42113

static TIFFExtendProc parent_tag_extender;

static void tag_extender(TIFF *tif) {
    static const TIFFFieldInfo info[] = {
        {TAG_GDAL_NODATA, -1, -1, TIFF_ASCII, FIELD_CUSTOM, 1, 0, (char *)"GDALNoDataValue"},
    };
    TIFFMergeFieldInfo(tif, info, 1);
    if (parent_tag_extender) parent_tag_extender(tif);
}

// One raw sample to float, the formats SIH's reader accepts.
static float sample_to_float(const uint8_t *p, uint16_t fmt, uint16_t bits) {
    union { uint8_t u8; int8_t i8; uint16_t u16; int16_t i16; uint32_t u32; int32_t i32; float f; double d; } v;
    memcpy(&v, p, bits / 8);
    if (fmt == SAMPLEFORMAT_UINT) return bits == 8 ? v.u8 : bits == 16 ? v.u16 : (float)v.u32;
    if (fmt == SAMPLEFORMAT_INT) return bits == 8 ? v.i8 : bits == 16 ? v.i16 : (float)v.i32;
    return bits == 32 ? v.f : (float)v.d;
}

bool dem_load_geotiff(const char *path, dem_grid_t *out) {
    memset(out, 0, sizeof *out);
    static bool registered;
    if (!registered) {
        registered = true;
        parent_tag_extender = TIFFSetTagExtender(tag_extender);
    }
    TIFF *tif = TIFFOpen(path, "r");
    if (!tif) return false;

    uint32_t w = 0, h = 0, n_scale = 0, n_tie = 0, n_keys = 0;
    uint16_t bits = 0, fmt = SAMPLEFORMAT_UINT, spp = 1;
    double *scale = NULL, *tie = NULL;
    uint16_t *keys = NULL;
    char *nodata_str = NULL;
    float *z = NULL;
    uint8_t *raw = NULL;
    bool ok = false;

    TIFFGetField(tif, TIFFTAG_IMAGEWIDTH, &w);
    TIFFGetField(tif, TIFFTAG_IMAGELENGTH, &h);
    TIFFGetField(tif, TIFFTAG_BITSPERSAMPLE, &bits);
    TIFFGetFieldDefaulted(tif, TIFFTAG_SAMPLEFORMAT, &fmt);
    TIFFGetFieldDefaulted(tif, TIFFTAG_SAMPLESPERPIXEL, &spp);

    bool integer = (fmt == SAMPLEFORMAT_UINT || fmt == SAMPLEFORMAT_INT) && (bits == 8 || bits == 16 || bits == 32);
    bool real = fmt == SAMPLEFORMAT_IEEEFP && (bits == 32 || bits == 64);
    if (w < 2 || h < 2 || spp != 1 || !(integer || real)) {
        fprintf(stderr, "dem: %s must be a single-band integer or float GeoTIFF\n", path);
        goto done;
    }
    const size_t bps = bits / 8;
    if (!TIFFGetField(tif, TAG_PIXEL_SCALE, &n_scale, &scale) || n_scale < 2 ||
        !TIFFGetField(tif, TAG_TIEPOINT, &n_tie, &tie) || n_tie < 6 ||
        !TIFFGetField(tif, TAG_GEOKEYS, &n_keys, &keys) || n_keys < 4) {
        fprintf(stderr, "dem: %s has no GeoTIFF georeferencing\n", path);
        goto done;
    }

    bool geographic = false, pixel_is_point = false;
    for (uint32_t k = 4; k + 3 < n_keys; k += 4) {
        if (keys[k] == 1024) geographic = (keys[k + 3] == 2);
        if (keys[k] == 1025) pixel_is_point = (keys[k + 3] == 2);
    }
    if (!geographic) {
        fprintf(stderr, "dem: %s is projected, convert with gdalwarp -t_srs EPSG:4326\n", path);
        goto done;
    }

    z = malloc((size_t)w * h * sizeof(float));
    if (!z) goto done;
    if (TIFFIsTiled(tif)) {
        uint32_t tw = 0, th = 0;
        TIFFGetField(tif, TIFFTAG_TILEWIDTH, &tw);
        TIFFGetField(tif, TIFFTAG_TILELENGTH, &th);
        raw = malloc((size_t)TIFFTileSize(tif));
        if (!raw || !tw || !th) goto done;
        for (uint32_t y = 0; y < h; y += th) {
            for (uint32_t x = 0; x < w; x += tw) {
                if (TIFFReadTile(tif, raw, x, y, 0, 0) < 0) goto done;
                uint32_t cols = (x + tw <= w) ? tw : w - x;
                for (uint32_t r = 0; r < th && y + r < h; r++)
                    for (uint32_t c = 0; c < cols; c++)
                        z[(size_t)(y + r) * w + x + c] = sample_to_float(raw + ((size_t)r * tw + c) * bps, fmt, bits);
            }
        }
    } else {
        raw = malloc((size_t)TIFFScanlineSize(tif));
        if (!raw) goto done;
        for (uint32_t r = 0; r < h; r++) {
            if (TIFFReadScanline(tif, raw, r, 0) < 0) goto done;
            for (uint32_t c = 0; c < w; c++)
                z[(size_t)r * w + c] = sample_to_float(raw + c * bps, fmt, bits);
        }
    }

    // the GDAL nodata value becomes NaN, matched the way SIH matches it
    if (TIFFGetField(tif, TAG_GDAL_NODATA, &nodata_str) && nodata_str) {
        char *end;
        float nodata = strtof(nodata_str, &end);
        if (end != nodata_str && !isnan(nodata)) {
            float tol = FLT_EPSILON * fmaxf(1.f, fabsf(nodata));
            for (size_t i = 0; i < (size_t)w * h; i++)
                if (fabsf(z[i] - nodata) <= tol) z[i] = NAN;
        }
    }

    out->nx = (int)w;
    out->ny = (int)h;
    out->dlon = scale[0];
    out->dlat = -scale[1];
    out->lon0 = tie[3] - tie[0] * scale[0];
    out->lat0 = tie[4] + tie[1] * scale[1];
    if (!pixel_is_point) {
        out->lon0 += 0.5 * out->dlon;
        out->lat0 += 0.5 * out->dlat;
    }
    out->z = z;
    z = NULL;
    ok = true;
    printf("dem: %s %ux%u cells, north-west %.6f %.6f, cell %.8f x %.8f deg\n",
           path, w, h, out->lat0, out->lon0, out->dlon, -out->dlat);

done:
    free(z);
    free(raw);
    TIFFClose(tif);
    return ok;
}
#else
bool dem_load_geotiff(const char *path, dem_grid_t *out) {
    (void)path;
    memset(out, 0, sizeof *out);
    fprintf(stderr, "dem: this build has no GeoTIFF reader, configure with -DHAWKEYE_GEOTIFF=ON\n");
    return false;
}
#endif

bool dem_sample(const dem_grid_t *g, double lat, double lon, float *z) {
    double fx = (lon - g->lon0) / g->dlon;
    double fy = (lat - g->lat0) / g->dlat;
    if (!(fx >= 0.0 && fy >= 0.0 && fx <= g->nx - 1 && fy <= g->ny - 1)) return false;
    int c = (int)fx, r = (int)fy;
    if (c > g->nx - 2) c = g->nx - 2;
    if (r > g->ny - 2) r = g->ny - 2;
    float tx = (float)(fx - c), ty = (float)(fy - r);
    const float *p = g->z + (size_t)r * g->nx + c;
    float v = (1.f - ty) * ((1.f - tx) * p[0] + tx * p[1]) + ty * ((1.f - tx) * p[g->nx] + tx * p[g->nx + 1]);
    if (isnan(v)) return false;
    *z = v;
    return true;
}

static int clampi(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }

bool dem_build_model(const dem_grid_t *g, double lat0_rad, double lon0_rad, double alt0,
                     float radius_m, Model *out) {
    const double lat0 = lat0_rad * 180.0 / M_PI, lon0 = lon0_rad * 180.0 / M_PI;
    const double m_lat = EARTH_RADIUS * M_PI / 180.0, m_lon = m_lat * cos(lat0_rad);

    int c0 = clampi((int)floor((lon0 - radius_m / m_lon - g->lon0) / g->dlon), 0, g->nx - 1);
    int c1 = clampi((int)ceil((lon0 + radius_m / m_lon - g->lon0) / g->dlon), 0, g->nx - 1);
    int r0 = clampi((int)floor((lat0 + radius_m / m_lat - g->lat0) / g->dlat), 0, g->ny - 1);
    int r1 = clampi((int)ceil((lat0 - radius_m / m_lat - g->lat0) / g->dlat), 0, g->ny - 1);
    int cols = c1 - c0 + 1, rows = r1 - r0 + 1;
    if (cols < 2 || rows < 2) return false;

    Vector3 *p = malloc((size_t)cols * rows * sizeof *p);
    Color *shade = malloc((size_t)cols * rows * sizeof *shade);
    if (!p || !shade) { free(p); free(shade); return false; }

    float zmin = INFINITY, zmax = -INFINITY;
    for (int r = 0; r < rows; r++) {
        for (int c = 0; c < cols; c++) {
            float z = g->z[(size_t)(r0 + r) * g->nx + (c0 + c)];
            double lat = g->lat0 + (r0 + r) * g->dlat, lon = g->lon0 + (c0 + c) * g->dlon;
            p[r * cols + c] = (Vector3){(float)((lon - lon0) * m_lon), (float)(z - alt0),
                                        (float)(-(lat - lat0) * m_lat)};
            if (z < zmin) zmin = z;
            if (z > zmax) zmax = z;
        }
    }

    // cells with no data take the mean of their neighbors, pass by pass inward, so the hole has a floor to mark
    unsigned char *fake = calloc((size_t)cols * rows, 1);
    float *fill = malloc((size_t)cols * rows * sizeof *fill);
    if (!fake || !fill) { free(p); free(shade); free(fake); free(fill); return false; }
    for (int pass = 0; pass < 32; pass++) {
        int filled = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                fill[r * cols + c] = NAN;
                if (!isnan(p[r * cols + c].y)) continue;
                float sum = 0.f; int n = 0;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        int rr = r + dr, cc = c + dc;
                        if (rr < 0 || rr >= rows || cc < 0 || cc >= cols || isnan(p[rr * cols + cc].y)) continue;
                        sum += p[rr * cols + cc].y; n++;
                    }
                }
                if (n) { fill[r * cols + c] = sum / n; filled++; }
            }
        }
        if (!filled) break;
        for (int i = 0; i < cols * rows; i++) {
            if (isnan(fill[i])) continue;
            p[i].y = fill[i];
            fake[i] = 1;
        }
    }
    free(fill);

    // the simulator reads the ground bilinearly between cell centers, so the mesh follows that
    // surface through SUB steps per cell instead of two flat triangles per cell
    enum { SUB = 4 };
    {
        const int fc = (cols - 1) * SUB + 1, fr = (rows - 1) * SUB + 1;
        Vector3 *fp = malloc((size_t)fc * fr * sizeof *fp);
        unsigned char *ff = malloc((size_t)fc * fr);
        if (!fp || !ff) { free(fp); free(ff); free(p); free(shade); free(fake); return false; }
        for (int r = 0; r < fr; r++) {
            const int r0 = r / SUB < rows - 1 ? r / SUB : rows - 2;
            const float ty = (float)(r - r0 * SUB) / SUB;
            for (int c = 0; c < fc; c++) {
                const int c0 = c / SUB < cols - 1 ? c / SUB : cols - 2;
                const float tx = (float)(c - c0 * SUB) / SUB;
                const Vector3 a = p[r0 * cols + c0], b = p[r0 * cols + c0 + 1],
                              d = p[(r0 + 1) * cols + c0], e = p[(r0 + 1) * cols + c0 + 1];
                const float y = (1 - ty) * ((1 - tx) * a.y + tx * b.y) + ty * ((1 - tx) * d.y + tx * e.y);
                fp[r * fc + c] = (Vector3){a.x + (b.x - a.x) * tx, y, a.z + (d.z - a.z) * ty};
                ff[r * fc + c] = fake[r0 * cols + c0] | fake[r0 * cols + c0 + 1] |
                                 fake[(r0 + 1) * cols + c0] | fake[(r0 + 1) * cols + c0 + 1];
            }
        }
        free(p); free(fake); free(shade);
        p = fp; fake = ff; cols = fc; rows = fr;
        shade = malloc((size_t)cols * rows * sizeof *shade);
        if (!shade) { free(p); free(fake); return false; }
    }

    // one light from above and a little east and south, a quarter ambient, hollows darkened
    const Vector3 light = Vector3Normalize((Vector3){0.30f, 0.80f, 0.40f});
    const float ambient = 0.25f;
    const float step_m = (float)(g->dlon * m_lon) / SUB;
    int quads = 0;
    for (int r = 0; r < rows; r++) {
        for (int c = 0; c < cols; c++) {
            Vector3 e = Vector3Subtract(p[r * cols + clampi(c + 1, 0, cols - 1)], p[r * cols + clampi(c - 1, 0, cols - 1)]);
            Vector3 s = Vector3Subtract(p[clampi(r + 1, 0, rows - 1) * cols + c], p[clampi(r - 1, 0, rows - 1) * cols + c]);
            float lambert = powf(fmaxf(0.f, Vector3DotProduct(Vector3Normalize(Vector3CrossProduct(s, e)), light)), 1.5f);
            float avg = 0.25f * (p[r * cols + clampi(c + 1, 0, cols - 1)].y + p[r * cols + clampi(c - 1, 0, cols - 1)].y
                               + p[clampi(r + 1, 0, rows - 1) * cols + c].y + p[clampi(r - 1, 0, rows - 1) * cols + c].y);
            float concavity = Clamp((avg - p[r * cols + c].y) / fmaxf(step_m, 1.f), -1.f, 1.f);
            float lit = (ambient * 0.5f + (1.f - ambient * 0.5f) * lambert) * (1.f - 0.6f * fmaxf(concavity, 0.f));
            unsigned char g = (unsigned char)(255.f * fminf(lit, 1.f));
            shade[r * cols + c] = (Color){g, g, g, 255};
            if (r + 1 < rows && c + 1 < cols && !isnan(p[r * cols + c].y) && !isnan(p[r * cols + c + 1].y) &&
                !isnan(p[(r + 1) * cols + c].y) && !isnan(p[(r + 1) * cols + c + 1].y)) quads++;
        }
    }
    if (quads == 0) { free(p); free(shade); free(fake); return false; }

    Mesh mesh = {0};
    mesh.triangleCount = quads * 2;
    mesh.vertexCount = quads * 6;
    mesh.vertices = MemAlloc((unsigned)mesh.vertexCount * 3 * sizeof(float));
    mesh.colors = MemAlloc((unsigned)mesh.vertexCount * 4);
    int v = 0;
    for (int r = 0; r + 1 < rows; r++) {
        for (int c = 0; c + 1 < cols; c++) {
            const int idx[6] = {r * cols + c, (r + 1) * cols + c, r * cols + c + 1,
                                r * cols + c + 1, (r + 1) * cols + c, (r + 1) * cols + c + 1};
            if (isnan(p[idx[0]].y) || isnan(p[idx[1]].y) || isnan(p[idx[2]].y) || isnan(p[idx[5]].y)) continue;
            for (int k = 0; k < 6; k++, v++) {
                memcpy(mesh.vertices + v * 3, &p[idx[k]], 3 * sizeof(float));
                memcpy(mesh.colors + v * 4, &shade[idx[k]], 4);
            }
        }
    }
    free(p);
    free(shade);
    free(fake);
    UploadMesh(&mesh, false);
    *out = LoadModelFromMesh(mesh);
    printf("dem: drew %d x %d cells, %d triangles, ground %.1f to %.1f m\n", cols, rows, mesh.triangleCount, zmin, zmax);
    return true;
}
