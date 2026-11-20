#ifndef TERRAIN_DEM_H
#define TERRAIN_DEM_H

#include <stdbool.h>
#include "raylib.h"

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

#endif
