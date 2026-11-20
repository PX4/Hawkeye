#ifndef TERRAIN_DEM_H
#define TERRAIN_DEM_H

#include <stdbool.h>
#include "raylib.h"
#include "theme.h"

// Elevation grid regular in degrees. Sample (0,0) is the north-west cell center.
typedef struct {
    float *z;          // meters above sea level, row-major, NaN where there is no data
    int nx, ny;
    double lon0, lat0; // degrees, center of sample (0,0)
    double dlon, dlat; // degrees per column and per row, dlat is negative
} dem_grid_t;

// Reads a single-band GeoTIFF, integer or float samples, in plain latitude and longitude.
bool dem_load_geotiff(const char *path, dem_grid_t *out);

// Bilinear height at a point in degrees. False outside the grid or over missing data.
bool dem_sample(const dem_grid_t *g, double lat, double lon, float *z);

// Mesh of the cells within radius_m of the origin, in the scene frame (x east, y up, z south).
bool dem_build_model(const dem_grid_t *g, double lat0_rad, double lon0_rad, double alt0,
                     float radius_m, Model *out);

// The lowest and highest ground in the built mesh, meters above sea level.
void dem_drawn_range(float *zmin, float *zmax);

// What the shader paints with, the lowest drawn ground in scene y, the meters per band, ten bands, and the cell size.
void dem_shader_scale(float *y_min, float *band_m, float *cell_m);

// The ramp color of band k of ten.
Color dem_band_color(const Color ramp[THEME_ELEV_RAMP_STOPS], int k);

// Where a contour value is written, one per level per 400 m of ground.
typedef struct { Vector3 at; float z; int level; } dem_label_t;
int dem_contour_labels(const dem_label_t **out);

#endif
