#include "raylib.h"
#include "raymath.h"
#include "asset_path.h"
#include "scene.h"
#include "vehicle.h"
#include "data_source.h"
#include "hud.h"
#include "live_marker.h"
#include "replay_control.h"
#include "replay_conflict.h"
#include "swarm_control.h"
#include "wheel_gesture.h"
#include <android/asset_manager.h>
#include <android/input.h>
#include <android/keycodes.h>
#include <android/log.h>
#include <android_native_app_glue.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <sys/stat.h>
#include <sys/types.h>

// Bump this string whenever APK assets change to force re-extraction on next launch.
#define HAWKEYE_ASSET_VERSION "2"
#define HAWKEYE_MAVLINK_PORT 19410
#define MAX_PATH_LEN ASSET_MAX_PATH

// Per-platform cap, mirroring MAX_VEHICLES in main.c / wasm_main.c (each platform main
// owns its own arrays; the shared C never sees the constant). The Kotlin library screen
// enforces the same cap (ReplayLibraryViewModel.MAX_SWARM).
#define MAX_SWARM_VEHICLES 16

#define TOUCH_ORBIT_SENSITIVITY  0.005f
#define TOUCH_ZOOM_SENSITIVITY   0.01f
#define TOUCH_PAN_SENSITIVITY    0.002f
#define TOUCH_MIN_ORBIT_DIST     2.0f
#define TOUCH_MAX_ORBIT_DIST    50.0f

// Forward-declare Raylib's Android accessor (defined in rcore_android.c, not in raylib.h).
struct android_app *GetAndroidApp(void);

// Raylib returns 0 for volume keys, letting Android show the system volume overlay.
// Wrap the input handler to consume them so the overlay never appears.
static int32_t (*orig_input_handler)(struct android_app *, AInputEvent *) = NULL;

static int32_t input_handler(struct android_app *app, AInputEvent *event) {
    if (AInputEvent_getType(event) == AINPUT_EVENT_TYPE_KEY) {
        int32_t kc = AKeyEvent_getKeyCode(event);
        if (kc == AKEYCODE_VOLUME_UP || kc == AKEYCODE_VOLUME_DOWN)
            return 1;
    }
    if (!orig_input_handler) {
        __android_log_print(ANDROID_LOG_WARN, "Hawkeye", "input_handler: orig is NULL, all input dropped");
        return 0;
    }
    return orig_input_handler(app, event);
}

// Android-specific asset_path implementation — all assets live under internalDataPath
// after extract_assets() copies them from the APK on first launch.
static char s_internal_data_path[MAX_PATH_LEN];

// Raylib's utils.h redefines fopen as android_fopen for all Android code. android_fopen
// doesn't handle absolute paths: its fallback prepends internalDataPath to an already-
// absolute path, producing a doubly-nested garbage path. These callbacks are registered
// with Raylib so it calls into our code (no utils.h macro here) for all file I/O, letting
// us open the extracted files directly with the real fopen.
static char *read_text_from_disk(const char *fileName) {
    FILE *f = fopen(fileName, "rt");
    if (!f) return NULL;
    fseek(f, 0, SEEK_END);
    long size = ftell(f);
    fseek(f, 0, SEEK_SET);
    if (size <= 0) { fclose(f); return NULL; }
    char *buf = (char *)MemAlloc((unsigned int)(size + 1));
    if (!buf) { fclose(f); return NULL; }
    size_t n = fread(buf, 1, (size_t)size, f);
    buf[n] = '\0';
    fclose(f);
    return buf;
}

static unsigned char *read_data_from_disk(const char *fileName, int *dataSize) {
    FILE *f = fopen(fileName, "rb");
    if (!f) { *dataSize = 0; return NULL; }
    fseek(f, 0, SEEK_END);
    long size = ftell(f);
    fseek(f, 0, SEEK_SET);
    if (size <= 0) { fclose(f); *dataSize = 0; return NULL; }
    unsigned char *buf = (unsigned char *)MemAlloc((unsigned int)size);
    if (!buf) { fclose(f); *dataSize = 0; return NULL; }
    *dataSize = (int)fread(buf, 1, (size_t)size, f);
    fclose(f);
    return buf;
}

// Creates the immediate parent directory of path. Assumes all ancestor directories
// already exist (single mkdir, not recursive).
static void ensure_parent_dir(const char *path) {
    char tmp[MAX_PATH_LEN];
    snprintf(tmp, sizeof(tmp), "%s", path);
    char *slash = strrchr(tmp, '/');
    if (slash) { *slash = '\0'; mkdir(tmp, 0755); }
}

static bool save_data_to_disk(const char *fileName, void *data, int dataSize) {
    ensure_parent_dir(fileName);
    FILE *f = fopen(fileName, "wb");
    if (!f) return false;
    bool ok = ((int)fwrite(data, 1, (size_t)dataSize, f) == dataSize);
    fclose(f);
    return ok;
}

static bool save_text_to_disk(const char *fileName, char *text) {
    ensure_parent_dir(fileName);
    FILE *f = fopen(fileName, "wt");
    if (!f) return false;
    bool ok = (fputs(text, f) >= 0);
    fclose(f);
    return ok;
}

// No-op: s_internal_data_path is set in main() before any asset_path() calls.
void asset_path_init(void) {}

void asset_path(const char *subpath, char *out, size_t out_size) {
    snprintf(out, out_size, "%s/%s", s_internal_data_path, subpath);
}

void asset_write_path(const char *subpath, char *out, size_t out_size) {
    snprintf(out, out_size, "%s/%s", s_internal_data_path, subpath);
    // Create the parent directory so callers can write without a separate mkdir.
    char tmp[MAX_PATH_LEN];
    snprintf(tmp, sizeof(tmp), "%s", out);
    char *last_slash = strrchr(tmp, '/');
    if (last_slash) { *last_slash = '\0'; mkdir(tmp, 0755); }
}

// Patch GLSL source from desktop (#version 330) to OpenGL ES (#version 300 es).
// Fragment shaders also get a precision qualifier injected after the version line.
// Returns a malloc'd buffer the caller must free; src_len bytes are consumed.
static char *patch_glsl(const char *filename, const char *src, size_t src_len, size_t *out_len) {
    size_t fn_len = strlen(filename);
    int is_fs = (fn_len >= 3 && strcmp(filename + fn_len - 3, ".fs") == 0);

    const char *old_ver  = "#version 330";
    const char *new_ver  = "#version 300 es";
    size_t old_len  = strlen(old_ver);
    // src is NOT NUL-terminated (raw APK asset bytes) — use memmem, not strstr.
    const char *pos = memmem(src, src_len, old_ver, old_len);
    if (!pos) {
        char *copy = malloc(src_len + 1);
        if (!copy) return NULL;
        memcpy(copy, src, src_len);
        copy[src_len] = '\0';
        *out_len = src_len;
        return copy;
    }

    const char *precision = "\nprecision mediump float;";
    size_t new_len  = strlen(new_ver);
    size_t prec_len = is_fs ? strlen(precision) : 0;
    size_t total    = src_len - old_len + new_len + prec_len;

    char *out = malloc(total + 1);
    if (!out) return NULL;
    size_t prefix = (size_t)(pos - src);
    memcpy(out, src, prefix);
    memcpy(out + prefix, new_ver, new_len);
    if (is_fs) memcpy(out + prefix + new_len, precision, prec_len);
    memcpy(out + prefix + new_len + prec_len, pos + old_len, src_len - prefix - old_len);
    out[total] = '\0';
    *out_len = total;
    return out;
}

static int write_file(const char *path, const void *data, size_t len) {
    FILE *f = fopen(path, "wb");
    if (!f) {
        __android_log_print(ANDROID_LOG_ERROR, "Hawkeye", "extract: failed to write %s: %s", path, strerror(errno));
        return 0;
    }
    size_t written = fwrite(data, 1, len, f);
    fclose(f);
    if (written != len) {
        __android_log_print(ANDROID_LOG_ERROR, "Hawkeye", "extract: short write %s: %zu of %zu bytes", path, written, len);
        return 0;
    }
    return 1;
}

// Copy APK assets to internalDataPath so fopen() can read them directly.
// When force=false, skips files that already exist (only-if-missing policy).
// When force=true, overwrites existing files (used after a version bump).
// Shader files are GLSL-patched for OpenGL ES during the write.
// Returns 1 if all files were written successfully, 0 if any file failed.
static int extract_assets(AAssetManager *mgr, const char *dst_root, int force) {
    static const char *dirs[] = { "fonts", "models", "shaders", "themes" };
    int ok = 1;

    for (int d = 0; d < (int)(sizeof(dirs) / sizeof(dirs[0])); d++) {
        char subdir[MAX_PATH_LEN];
        snprintf(subdir, sizeof(subdir), "%s/%s", dst_root, dirs[d]);
        mkdir(subdir, 0755);

        AAssetDir *dir = AAssetManager_openDir(mgr, dirs[d]);
        if (!dir) continue;

        const char *fname;
        while ((fname = AAssetDir_getNextFileName(dir)) != NULL) {
            char dest[MAX_PATH_LEN];
            snprintf(dest, sizeof(dest), "%s/%s", subdir, fname);

            struct stat st;
            if (!force && stat(dest, &st) == 0) continue;

            char rel[MAX_PATH_LEN];
            snprintf(rel, sizeof(rel), "%s/%s", dirs[d], fname);

            AAsset *asset = AAssetManager_open(mgr, rel, AASSET_MODE_BUFFER);
            if (!asset) {
                __android_log_print(ANDROID_LOG_ERROR, "Hawkeye", "extract: AAssetManager_open failed for %s", rel);
                ok = 0;
                continue;
            }

            const void *buf = AAsset_getBuffer(asset);
            off_t len       = AAsset_getLength(asset);

            if (!buf || len <= 0) {
                __android_log_print(ANDROID_LOG_ERROR, "Hawkeye",
                    "extract: AAsset_getBuffer NULL for %s (compressed?)", rel);
                ok = 0;
                AAsset_close(asset);
                continue;
            }

            {
                size_t fn_len = strlen(fname);
                int is_shader = fn_len >= 3 &&
                    (strcmp(fname + fn_len - 3, ".vs") == 0 ||
                     strcmp(fname + fn_len - 3, ".fs") == 0);

                if (is_shader) {
                    size_t patched_len;
                    char *patched = patch_glsl(fname, (const char *)buf, (size_t)len, &patched_len);
                    if (patched) {
                        if (!write_file(dest, patched, patched_len)) ok = 0;
                        free(patched);
                    } else {
                        __android_log_print(ANDROID_LOG_ERROR, "Hawkeye", "extract: patch_glsl failed for %s", rel);
                        ok = 0;
                    }
                } else {
                    int primary_ok = write_file(dest, buf, (size_t)len);
                    if (!primary_ok) ok = 0;
                    // tinyobj opens MTL files by bare filename via android_fopen, whose fallback
                    // constructs internalDataPath/<basename>. Mirror MTL files to the root so
                    // that path resolves without any fopen wrapping.
                    if (primary_ok && fn_len >= 5 && strcmp(fname + fn_len - 4, ".mtl") == 0) {
                        char flat[MAX_PATH_LEN];
                        snprintf(flat, sizeof(flat), "%s/%s", dst_root, fname);
                        struct stat flat_st;
                        if (force || stat(flat, &flat_st) != 0)
                            if (!write_file(flat, buf, (size_t)len)) ok = 0;
                    }
                }
            }

            AAsset_close(asset);
        }

        AAssetDir_close(dir);
    }
    return ok;
}

static void handle_touch(Camera3D *cam, Vector3 *orbit_target,
                          int *prev_count, Vector2 *prev_touch,
                          float *prev_pinch_dist, Vector2 *prev_mid) {
    int count = GetTouchPointCount();

    // On finger count change: reset prev values to current to avoid a jump.
    if (count != *prev_count) {
        if (count >= 1) *prev_touch = GetTouchPosition(0);
        if (count >= 2) {
            Vector2 t0 = GetTouchPosition(0);
            Vector2 t1 = GetTouchPosition(1);
            *prev_pinch_dist = Vector2Distance(t0, t1);
            *prev_mid = (Vector2){ (t0.x + t1.x) * 0.5f, (t0.y + t1.y) * 0.5f };
        }
        *prev_count = count;
        return;
    }

    if (count == 1) {
        Vector2 touch = GetTouchPosition(0);
        Vector2 delta = { touch.x - prev_touch->x, touch.y - prev_touch->y };
        *prev_touch = touch;

        Vector3 offset = Vector3Subtract(cam->position, *orbit_target);
        float r = Vector3Length(offset);
        if (r < 0.001f) return;

        // Yaw: rotate offset around world Y
        Matrix yaw = MatrixRotate((Vector3){0, 1, 0}, -delta.x * TOUCH_ORBIT_SENSITIVITY);
        offset = Vector3Transform(offset, yaw);

        // Pitch: rotate offset around camera right axis
        Vector3 forward = Vector3Normalize(Vector3Negate(offset));
        Vector3 right   = Vector3Normalize(Vector3CrossProduct(forward, (Vector3){0, 1, 0}));
        Matrix  pitch_m = MatrixRotate(right, -delta.y * TOUCH_ORBIT_SENSITIVITY);
        offset = Vector3Transform(offset, pitch_m);

        // Clamp elevation to ±89° to prevent gimbal flip
        float len = Vector3Length(offset);
        if (len < 0.001f) return;
        float elevation = asinf(Clamp(offset.y / len, -1.0f, 1.0f));
        float max_elev  = 89.0f * DEG2RAD;
        if (elevation > max_elev || elevation < -max_elev) {
            float azimuth = atan2f(offset.x, offset.z);
            elevation = Clamp(elevation, -max_elev, max_elev);
            offset.x  = len * cosf(elevation) * sinf(azimuth);
            offset.y  = len * sinf(elevation);
            offset.z  = len * cosf(elevation) * cosf(azimuth);
        }

        cam->position = Vector3Add(*orbit_target, Vector3Scale(Vector3Normalize(offset), r));
        cam->target   = *orbit_target;
        cam->up       = (Vector3){0, 1, 0};
    } else if (count == 2) {
        cam->up = (Vector3){0, 1, 0};

        Vector2 t0  = GetTouchPosition(0);
        Vector2 t1  = GetTouchPosition(1);
        Vector2 mid = { (t0.x + t1.x) * 0.5f, (t0.y + t1.y) * 0.5f };
        float   dist = Vector2Distance(t0, t1);

        // Zoom: scale orbit radius proportionally so speed feels consistent at any distance
        float   dist_delta = dist - *prev_pinch_dist;
        Vector3 offset     = Vector3Subtract(cam->position, *orbit_target);
        float   r          = Vector3Length(offset);
        r *= (1.0f - dist_delta * TOUCH_ZOOM_SENSITIVITY);
        if (r < TOUCH_MIN_ORBIT_DIST) r = TOUCH_MIN_ORBIT_DIST;
        if (r > TOUCH_MAX_ORBIT_DIST) r = TOUCH_MAX_ORBIT_DIST;
        if (r > 0.001f)
            cam->position = Vector3Add(*orbit_target,
                                       Vector3Scale(Vector3Normalize(offset), r));

        // Pan: translate orbit_target along camera right and up
        Vector2 mid_delta = { mid.x - prev_mid->x, mid.y - prev_mid->y };
        float   pan_sens  = r * TOUCH_PAN_SENSITIVITY;
        Vector3 forward   = Vector3Normalize(Vector3Subtract(cam->target, cam->position));
        Vector3 right     = Vector3Normalize(Vector3CrossProduct(forward, cam->up));
        Vector3 pan       = Vector3Add(
            Vector3Scale(right,    -mid_delta.x * pan_sens),
            Vector3Scale(cam->up,   mid_delta.y * pan_sens)
        );
        *orbit_target = Vector3Add(*orbit_target, pan);
        cam->position = Vector3Add(cam->position, pan);
        cam->target   = *orbit_target;

        *prev_pinch_dist = dist;
        *prev_mid        = mid;
        *prev_touch      = GetTouchPosition(0);
    }
}

// The library repository (Kotlin) stages one or more .ulg files via .tmp + atomic
// rename — inbox/current.ulg for the first (or only) log, inbox/swarm_<i>.ulg for the
// rest of a swarm session — then writes a fresh token into inbox/.ready. We poll the
// sentinel's *contents* (not stat-mtime — f2fs has 1-second granularity and would
// coalesce two stages in the same wall second) once per second and reload when the
// token changes.
static data_source_t g_sources[MAX_SWARM_VEHICLES];
static int           g_source_count = 0;   // 0 = no session loaded
static int           g_selected = 0;       // drone the camera and HUD follow
static bool          g_live_mode = false;
static bool          g_ghost_mode = false; // multi-replay auto-resolved to ghost layout
static bool          g_has_tier3 = false;  // any drone without a valid parsed home
static long long     g_last_ready_token = 0;
static hud_t         g_hud;
// Initialized to a large negative value so the first poll always proceeds.
static double        g_last_poll_time = -1e9;

// For camera-follow: each frame we translate orbit_target and camera.position
// by (selected vehicle position - g_last_vehicle_pos). Initialized at startup,
// and re-seeded on every reload so the first delta is zero. A drone switch via
// the wheel leaves it pointing at the old drone's position on purpose: the next
// frame's delta then carries the camera to the new drone with the user's orbit
// offset intact.
static Vector3       g_last_vehicle_pos = {0};

// Tap-and-hold detector for the swarm drone-selection wheel (shared header,
// platform-fed). Armed only for multi-drone replay sessions.
static wheel_gesture_t g_wheel = {0};

// Returns the millis token in the sentinel file, or 0 on missing/empty/parse-fail.
// The token is "<millis>" for a single log or "<millis> <count>" for a swarm batch;
// *out_count gets the validated count (default 1).
static long long read_ready_token(const char *path, int *out_count) {
    *out_count = 1;
    FILE *f = fopen(path, "rb");
    if (!f) return 0;
    char buf[64];
    size_t n = fread(buf, 1, sizeof(buf) - 1, f);
    fclose(f);
    if (n == 0) return 0;
    buf[n] = '\0';
    char *end = NULL;
    long long val = strtoll(buf, &end, 10);
    if (end == buf) return 0;
    long count = strtol(end, NULL, 10);
    if (count >= 1 && count <= MAX_SWARM_VEHICLES) *out_count = (int)count;
    return val;
}

// Reads the live marker file and parses "<millis> [port]" via parse_live_marker
// (live_marker.h). Returns the millis token (0 on missing/empty/parse-fail) and
// writes a validated listen port (fallback HAWKEYE_MAVLINK_PORT) through out_port.
static long long read_live_marker(const char *path, uint16_t *out_port) {
    *out_port = HAWKEYE_MAVLINK_PORT;
    FILE *f = fopen(path, "rb");
    if (!f) return 0;
    char buf[64];
    size_t n = fread(buf, 1, sizeof(buf) - 1, f);
    fclose(f);
    if (n == 0) return 0;
    buf[n] = '\0';
    return parse_live_marker(buf, HAWKEYE_MAVLINK_PORT, out_port);
}

// Applies the auto-resolved multi-drone layout. A trimmed adaptation of wasm_main.c's
// apply_replay_mode (P-key dialog variant), using home-based origins like the existing
// single-log path: vehicle_update's first-sample origin latch can catch a transient
// lat=0 state, while the ULog pre-scan's home is reliable.
//   1 = formation (shared NED origin, real relative positions)
//   2 = ghost     (own home origin, non-primary drones at 0.35 alpha)
//   3 = grid      (own home origin, +5 m X offset per drone)
static void apply_swarm_layout(vehicle_t *vehicles, int count,
                               double ref_lat_rad, double ref_lon_rad,
                               double min_alt, int mode) {
    for (int i = 0; i < count; i++) {
        vehicles[i].grid_offset = (Vector3){0, 0, 0};
        vehicle_set_ghost_alpha(&vehicles[i], 1.0f);

        if (mode == 1) {
            if (g_sources[i].home.valid) {
                vehicles[i].lat0 = ref_lat_rad;
                vehicles[i].lon0 = ref_lon_rad;
                vehicles[i].alt0 = min_alt;
                vehicles[i].origin_set = true;
            }
        } else if (g_sources[i].home.valid) {
            vehicles[i].lat0 = g_sources[i].home.lat * 1e-7 * (M_PI / 180.0);
            vehicles[i].lon0 = g_sources[i].home.lon * 1e-7 * (M_PI / 180.0);
            vehicles[i].alt0 = g_sources[i].home.alt * 1e-3;
            vehicles[i].origin_set = true;
        }
        // No valid home: leave origin unset for vehicle_update's wait-and-latch.
    }

    if (mode == 2) {
        for (int i = 1; i < count; i++)
            vehicle_set_ghost_alpha(&vehicles[i], 0.35f);
    } else if (mode == 3) {
        for (int i = 1; i < count; i++)
            vehicles[i].grid_offset = (Vector3){ i * 5.0f, 0.0f, 0.0f };
    }
}

static int try_load_inbox_swarm(const scene_t *scene, vehicle_t *vehicles,
                                int *vehicle_inited_count) {
    // Throttle to 1 Hz. Sentinel changes are user-driven (library playback), so
    // a per-frame stat()/read() at 60 Hz is wasted work — and since we read
    // the sentinel's contents (not its mtime), an up-to-1-second detection
    // delay is the only cost.
    double now = GetTime();
    if (now - g_last_poll_time < 1.0) return 0;
    g_last_poll_time = now;

    char ready[MAX_PATH_LEN];
    snprintf(ready, sizeof(ready), "%s/inbox/.ready", s_internal_data_path);
    int count = 1;
    long long token = read_ready_token(ready, &count);
    if (token == 0 || token == g_last_ready_token) return 0;

    // Parse-then-swap: construct every new data source first. If any fails
    // (corrupt file, partial copy, etc.) the previous session keeps playing —
    // we don't tear down working state on speculation.
    data_source_t new_sources[MAX_SWARM_VEHICLES];
    memset(new_sources, 0, sizeof(new_sources));
    for (int i = 0; i < count; i++) {
        char ulg[MAX_PATH_LEN];
        if (i == 0)
            snprintf(ulg, sizeof(ulg), "%s/inbox/current.ulg", s_internal_data_path);
        else
            snprintf(ulg, sizeof(ulg), "%s/inbox/swarm_%d.ulg", s_internal_data_path, i);
        int rc = data_source_ulog_create(&new_sources[i], ulg);
        if (rc != 0) {
            __android_log_print(ANDROID_LOG_ERROR, "Hawkeye",
                "data_source_ulog_create(%s) failed: %d", ulg, rc);
            for (int j = 0; j < i; j++) data_source_close(&new_sources[j]);
            // Mark the bad token consumed so we don't retry every second; the
            // user must re-stage to trigger another attempt.
            g_last_ready_token = token;
            return 0;
        }
    }

    // Commit: swap the session over. The wheel machine resets too, so a gesture phase
    // from the old session can never linger into one where it is no longer ticked.
    for (int i = 0; i < g_source_count; i++) data_source_close(&g_sources[i]);
    for (int i = 0; i < count; i++) g_sources[i] = new_sources[i];
    g_source_count = count;
    g_selected = 0;
    g_wheel = (wheel_gesture_t){0};
    g_last_ready_token = token;

    // Vehicles: init slots a larger session needs, release slots a smaller one
    // frees. Loading models here causes a one-time hitch at session start, same
    // as desktop startup.
    for (int i = *vehicle_inited_count; i < count; i++)
        vehicle_init(&vehicles[i], MODEL_QUADROTOR, scene->lighting_shader);
    for (int i = count; i < *vehicle_inited_count; i++)
        vehicle_cleanup(&vehicles[i]);
    *vehicle_inited_count = count;

    for (int i = 0; i < count; i++) {
        vehicle_reset_trail(&vehicles[i]);
        // Multi sessions color drones from the theme palette (matches trails and
        // the Kotlin wheel); single sessions keep the default white tint.
        vehicles[i].color = (count > 1) ? scene->theme->drone_palette[i % 16] : WHITE;
        if (g_sources[i].mav_type != 0)
            vehicle_set_type(&vehicles[i], g_sources[i].mav_type);
        // Reset origin tracking so a replay without a valid home falls back to
        // vehicle_update's wait-and-latch logic instead of inheriting the
        // previous session's origin.
        vehicles[i].origin_set = false;
        vehicles[i].origin_wait_count = 0;
    }

    if (count == 1) {
        g_ghost_mode = false;
        g_has_tier3 = false;
        // Pre-seed origin from the parsed home so positions are computed relative
        // to home. vehicle_update's own first-sample origin-init latches onto
        // whatever state it sees first — if state.lat is briefly 0 (drone powered
        // on, no GPS lock yet), it sets lat0=0 and the rest of the flight renders
        // at absolute lat/lon coordinates millions of meters from origin. The ULog
        // pre-scan populates ctx->home reliably, so use it when available.
        if (g_sources[0].home.valid) {
            vehicles[0].lat0 = g_sources[0].home.lat * 1e-7 * (M_PI / 180.0);
            vehicles[0].lon0 = g_sources[0].home.lon * 1e-7 * (M_PI / 180.0);
            vehicles[0].alt0 = g_sources[0].home.alt * 1e-3;
            vehicles[0].origin_set = true;
        }
    } else {
        // Shared NED reference: first valid home for lat/lon, lowest home for alt
        // (mirrors wasm_main.c's finalize).
        double ref_lat_rad = 0.0, ref_lon_rad = 0.0, min_alt = 1e9;
        for (int i = 0; i < count; i++) {
            if (g_sources[i].home.valid) {
                ref_lat_rad = g_sources[i].home.lat * 1e-7 * (M_PI / 180.0);
                ref_lon_rad = g_sources[i].home.lon * 1e-7 * (M_PI / 180.0);
                break;
            }
        }
        for (int i = 0; i < count; i++) {
            if (g_sources[i].home.valid) {
                double a = g_sources[i].home.alt * 1e-3;
                if (a < min_alt) min_alt = a;
            }
        }
        if (min_alt > 1e8) min_alt = 0.0;

        g_has_tier3 = false;
        for (int i = 0; i < count; i++) {
            if (!g_sources[i].home.valid) { g_has_tier3 = true; break; }
        }

        // Auto-resolve the layout (no dialog on Android): overlapping homes
        // render as ghosts, far-apart homes side by side on a grid, otherwise
        // formation with real relative positions.
        conflict_result_t cr = replay_detect_conflict(g_sources, count);
        int mode = !cr.conflict_detected ? 1 : (cr.conflict_far ? 3 : 2);
        g_ghost_mode = (mode == 2);
        apply_swarm_layout(vehicles, count, ref_lat_rad, ref_lon_rad, min_alt, mode);
        __android_log_print(ANDROID_LOG_INFO, "Hawkeye",
            "swarm layout: mode=%d conflict=%d far=%d tier3=%d",
            mode, cr.conflict_detected, cr.conflict_far, g_has_tier3);
    }

    // Re-seed the follow baseline so the first frame's delta covers the jump
    // from the previous session's last position (or the dummy startup position)
    // to this session's first valid sample — keeps the selected drone in view.
    g_last_vehicle_pos = vehicles[0].position;

    __android_log_print(ANDROID_LOG_INFO, "Hawkeye",
        "loaded %d ulg file(s): duration=%.1fs token=%lld",
        count, g_sources[0].playback.duration_s, token);
    return 1;
}

// ---------------------------------------------------------------------------
// Edge indicator chevrons for off-screen drones. Ported verbatim from
// wasm_main.c:120-201 (itself a port of main.c:62-145); each platform main
// carries its own copy because the helper draws with that platform's screen
// metrics and the shared C layer stays untouched.
// ---------------------------------------------------------------------------

static void draw_edge_indicators(const vehicle_t *vehicles, int vehicle_count,
                                  int selected, Camera3D camera, Font font)
{
    int ei_sw = GetScreenWidth();
    int ei_sh = GetScreenHeight();
    float ei_margin = 40.0f;
    float ei_scale = powf(ei_sh / 720.0f, 0.7f);
    if (ei_scale < 1.0f) ei_scale = 1.0f;
    Vector3 cam_fwd = Vector3Normalize(Vector3Subtract(
        camera.target, camera.position));

    for (int i = 0; i < vehicle_count; i++) {
        if (i == selected || !vehicles[i].active) continue;

        Vector3 to_drone = Vector3Subtract(vehicles[i].position,
                                            camera.position);
        float dot = to_drone.x * cam_fwd.x + to_drone.y * cam_fwd.y
                    + to_drone.z * cam_fwd.z;

        Vector2 sp = GetWorldToScreen(vehicles[i].position, camera);

        if (sp.x >= ei_margin && sp.x <= ei_sw - ei_margin &&
            sp.y >= ei_margin && sp.y <= ei_sh - ei_margin) continue;

        float ei_cx = ei_sw / 2.0f;
        float ei_cy = ei_sh / 2.0f;
        float ei_dx = sp.x - ei_cx;
        float ei_dy = sp.y - ei_cy;

        if (dot < 0.5f) {
            Vector3 cam_right = Vector3Normalize(
                Vector3CrossProduct(cam_fwd, (Vector3){0, 1, 0}));
            Vector3 cam_up_approx = Vector3CrossProduct(cam_right, cam_fwd);
            ei_dx = Vector3DotProduct(to_drone, cam_right);
            ei_dy = -Vector3DotProduct(to_drone, cam_up_approx);
            float len = sqrtf(ei_dx * ei_dx + ei_dy * ei_dy);
            if (len > 0.01f) { ei_dx /= len; ei_dy /= len; }
            ei_dx *= ei_sw;
            ei_dy *= ei_sh;
        }

        float sx = (ei_dx != 0)
            ? ((ei_dx > 0 ? ei_sw - ei_margin : ei_margin) - ei_cx) / ei_dx
            : 1e9f;
        float sy = (ei_dy != 0)
            ? ((ei_dy > 0 ? ei_sh - ei_margin : ei_margin) - ei_cy) / ei_dy
            : 1e9f;
        float se = fminf(fabsf(sx), fabsf(sy));
        float ex = ei_cx + ei_dx * se;
        float ey = ei_cy + ei_dy * se;
        if (ex < ei_margin) ex = ei_margin;
        if (ex > ei_sw - ei_margin) ex = ei_sw - ei_margin;
        if (ey < ei_margin) ey = ei_margin;
        if (ey > ei_sh - ei_margin) ey = ei_sh - ei_margin;

        Color col = vehicles[i].color;
        col.a = 220;
        float angle = atan2f(ei_dy, ei_dx);
        float sz = 14.0f * ei_scale;

        float chev_len = sz * 1.2f;
        float chev_spread = 0.5f;
        Vector2 tip = { ex + cosf(angle) * chev_len,
                        ey + sinf(angle) * chev_len };
        Vector2 cl = { ex + cosf(angle + chev_spread) * sz * 0.6f,
                       ey + sinf(angle + chev_spread) * sz * 0.6f };
        Vector2 cr = { ex + cosf(angle - chev_spread) * sz * 0.6f,
                       ey + sinf(angle - chev_spread) * sz * 0.6f };
        DrawLineEx(tip, cl, 2.5f * ei_scale, col);
        DrawLineEx(tip, cr, 2.5f * ei_scale, col);

        char num[4];
        snprintf(num, sizeof(num), "%d", i + 1);
        float lfs = 18.0f * ei_scale;
        Vector2 tw = MeasureTextEx(font, num, lfs, 0.5f);
        float lx = ex - cosf(angle) * (sz * 0.3f) - tw.x / 2;
        float ly = ey - sinf(angle) * (sz * 0.3f) - tw.y / 2;
        DrawTextEx(font, num, (Vector2){ lx, ly }, lfs, 0.5f, col);
    }
}

// Raylib's rcore_android.c owns android_main and calls user main() after platform setup.
int main(int argc, char *argv[]) {
    (void)argc; (void)argv;
    // 0, 0 = use the device's native screen resolution (fullscreen)
    InitWindow(0, 0, "Hawkeye");
    struct android_app *android_app = GetAndroidApp();
    snprintf(s_internal_data_path, sizeof(s_internal_data_path),
             "%s", android_app->activity->internalDataPath);
    char sentinel[MAX_PATH_LEN];
    snprintf(sentinel, sizeof(sentinel), "%s/.hawkeye_assets_v%s",
             s_internal_data_path, HAWKEYE_ASSET_VERSION);
    struct stat sentinel_st;
    int needs_extract = (stat(sentinel, &sentinel_st) != 0); /* force-overwrite when no sentinel */
    int extract_ok = extract_assets(android_app->activity->assetManager, s_internal_data_path, needs_extract);
    if (needs_extract && extract_ok) {
        FILE *sf = fopen(sentinel, "wb");
        if (sf) fclose(sf);
    }
    SetLoadFileTextCallback(read_text_from_disk);
    SetLoadFileDataCallback(read_data_from_disk);
    SetSaveFileDataCallback(save_data_to_disk);
    SetSaveFileTextCallback(save_text_to_disk);

    orig_input_handler = android_app->onInputEvent;
    android_app->onInputEvent = input_handler;
    SetTargetFPS(60);

    SetGesturesEnabled(GESTURE_DRAG | GESTURE_PINCH_IN | GESTURE_PINCH_OUT);

    scene_t scene = {0};
    scene_init(&scene);

    // Slot 0 is always initialized; a swarm session lazily initializes the rest
    // (and releases them again when a smaller session loads).
    vehicle_t vehicles[MAX_SWARM_VEHICLES] = {0};
    int vehicle_inited_count = 1;
    vehicle_init(&vehicles[0], MODEL_QUADROTOR, scene.lighting_shader);

    hud_init(&g_hud);
    // The transport bar is driven by a Compose overlay on Android, so suppress the
    // native one (keeps the rest of the HUD: instruments, telemetry, annunciators).
    g_hud.show_transport = false;
    // Bump HUD scale on mobile so values/labels meet M3 readability floors
    // (~42 px body, ~33 px label at this DPI). Desktop/WASM leave this at 1.0.
    g_hud.scale_mul = 1.5f;

    // Position vehicle slightly above origin so it's visible with the default camera
    vehicles[0].position = (Vector3){ 0.0f, 0.5f, 0.0f };

    scene.cam_mode   = CAM_MODE_FREE;
    scene.free_track = true;

    Vector3 orbit_target    = vehicles[0].position;
    Vector2 prev_touch      = {0};
    float   prev_pinch_dist = 0.0f;
    Vector2 prev_mid        = {0};
    int     prev_count      = 0;

    scene.camera.target = orbit_target;
    g_last_vehicle_pos  = vehicles[0].position;

    // Decide live vs replay from the inbox markers (newest token wins). The Kotlin
    // launcher writes inbox/.live for a Connect tap and inbox/.ready for a replay; a
    // fresh tap therefore beats any stale marker. live_port lives at main() scope so the
    // loop's live_status_publish can echo the actually-bound port back to the UI.
    uint16_t live_port = HAWKEYE_MAVLINK_PORT;
    {
        char live_path[MAX_PATH_LEN], ready_path[MAX_PATH_LEN];
        snprintf(live_path, sizeof(live_path), "%s/inbox/.live", s_internal_data_path);
        snprintf(ready_path, sizeof(ready_path), "%s/inbox/.ready", s_internal_data_path);
        long long live_token = read_live_marker(live_path, &live_port);
        int ready_count = 1;
        long long ready_token = read_ready_token(ready_path, &ready_count);
        // >= so an (unreachable) tie favors the explicit live intent; in practice the two
        // tokens come from distinct user actions stamped at different millis.
        if (live_token > 0 && live_token >= ready_token) {
            // The user explicitly tapped Connect, so commit to live mode regardless of bind
            // outcome. We never silently fall back to replay: the staged .ulg wasn't what the
            // user asked for, and the app suppresses the Compose transport overlay for live
            // sessions (HawkeyeActivity.isLiveMode), so a replay fallback would render with no
            // transport controls at all. On bind failure we stay in the live "Waiting…" state.
            g_live_mode = true;
            if (data_source_mavlink_create(&g_sources[0], live_port, /*channel=*/0, false) == 0) {
                g_source_count = 1;
                __android_log_print(ANDROID_LOG_INFO, "Hawkeye",
                    "live MAVLink: listening on UDP %u", live_port);
            } else {
                __android_log_print(ANDROID_LOG_ERROR, "Hawkeye",
                    "live MAVLink: failed to bind UDP %u (staying in live waiting state)",
                    live_port);
            }
        }
    }

    // Replay: pick up the session already staged into the inbox before native main() ran
    // (skipped in live mode so the inbox never overrides a live session).
    if (!g_live_mode) try_load_inbox_swarm(&scene, vehicles, &vehicle_inited_count);

    while (!WindowShouldClose()) {
        // Catch a new session staged while the renderer is already running (replay only).
        if (!g_live_mode) try_load_inbox_swarm(&scene, vehicles, &vehicle_inited_count);

        bool active = g_source_count > 0;

        // Consume a drone selection posted by the wheel overlay (JVM thread). The
        // camera-follow delta below then carries the camera to the new drone.
        int sel_req = swarm_control_take_select();
        if (sel_req >= 0 && sel_req < g_source_count) g_selected = sel_req;

        // Apply pending transport requests from the Compose overlay (JVM thread);
        // a seek moves every drone, so all trails restart together.
        if (replay_control_apply(g_sources, g_source_count, active)) {
            for (int i = 0; i < g_source_count; i++) vehicle_reset_trail(&vehicles[i]);
        }

        if (active) {
            float dt = GetFrameTime();
            for (int i = 0; i < g_source_count; i++) {
                data_source_poll(&g_sources[i], dt);
                vehicle_update(&vehicles[i], &g_sources[i].state, &g_sources[i].home);
            }
            if (g_live_mode) {
                static bool s_was_connected = false;
                if (g_sources[0].connected != s_was_connected) {
                    s_was_connected = g_sources[0].connected;
                    __android_log_print(ANDROID_LOG_INFO, "Hawkeye",
                        "live MAVLink: connected=%d sysid=%u",
                        g_sources[0].connected, g_sources[0].sysid);
                }
            }

            // Camera follow: translate the orbit center and the camera by the
            // selected vehicle's frame-to-frame motion so the user's orbit/pan
            // offset (relative to the vehicle) stays constant.
            if (g_sources[g_selected].state.valid) {
                Vector3 delta = Vector3Subtract(vehicles[g_selected].position,
                                                g_last_vehicle_pos);
                orbit_target = Vector3Add(orbit_target, delta);
                scene.camera.position = Vector3Add(scene.camera.position, delta);
                scene.camera.target = orbit_target;
                g_last_vehicle_pos = vehicles[g_selected].position;
            }
        }

        // Publish playback + live status for the Compose overlays to read via JNI.
        replay_control_publish(&g_sources[g_selected], active);
        live_status_publish(&g_sources[0], active, g_live_mode, live_port);

        // Tap-and-hold wheel gesture, armed only for multi-drone replay. Runs before
        // the camera handler: while the wheel owns the gesture (PENDING/OPEN) camera
        // input is suppressed; resetting prev_count makes handle_touch re-seed when
        // it takes over again, so neither hand-off jumps the camera.
        bool wheel_owns = false;
        {
            int touch_count = GetTouchPointCount();
            Vector2 touch = (touch_count > 0)
                ? GetTouchPosition(0)
                : (Vector2){ g_wheel.finger_x, g_wheel.finger_y };
            if (g_source_count >= 2 && !g_live_mode) {
                wheel_owns = wheel_gesture_update(&g_wheel, touch_count,
                                                  touch.x, touch.y, GetFrameTime());
            }
            swarm_control_publish(&g_wheel, g_source_count, g_selected);
        }
        if (wheel_owns) {
            prev_count = -1;
        } else {
            handle_touch(&scene.camera, &orbit_target,
                         &prev_count, &prev_touch,
                         &prev_pinch_dist, &prev_mid);
        }

        hud_update(&g_hud,
                   active ? g_sources[g_selected].state.time_usec : 0,
                   active ? g_sources[g_selected].connected : false,
                   GetFrameTime());

        BeginDrawing();
            scene_draw_sky(&scene);
            int trail_mode = active ? ((g_source_count > 1) ? 3 : 1) : 0;
            BeginMode3D(scene.camera);
                scene_draw(&scene);
                int draw_count = active ? g_source_count : 1;
                for (int i = 0; i < draw_count; i++) {
                    vehicle_draw(&vehicles[i], scene.theme,
                                 /*selected=*/i == g_selected,
                                 trail_mode,
                                 /*show_ground_track=*/false,
                                 /*cam_pos=*/scene.camera.position,
                                 /*classic_colors=*/false);
                }
            EndMode3D();

            if (g_source_count > 1) {
                draw_edge_indicators(vehicles, g_source_count, g_selected,
                                     scene.camera, g_hud.font_value);
            }

            // HUD overlay. When no .ulg is loaded, hand hud_draw a zeroed
            // data_source so the status row reads "Waiting…" without crashing.
            {
                data_source_t empty_src = {0};
                const data_source_t *src_ptr = active ? g_sources : &empty_src;
                int hud_count = active ? g_source_count : 1;
                int hud_selected = active ? g_selected : 0;
                // Marker overlays are not surfaced on Android: zeroed per-drone
                // entries keep hud_draw's indexing in bounds.
                hud_marker_data_t user_md[MAX_SWARM_VEHICLES] = {0};
                hud_marker_data_t sys_md[MAX_SWARM_VEHICLES] = {0};
                bool has_awaiting_gps = active && !vehicles[hud_selected].origin_set
                                        && g_sources[hud_selected].home.valid;
                if (g_hud.mode == HUD_CONSOLE) {
                    hud_draw(&g_hud, vehicles, src_ptr, hud_count, hud_selected,
                             GetScreenWidth(), GetScreenHeight(),
                             scene.theme, trail_mode,
                             user_md, sys_md, /*marker_vehicle_count=*/hud_count,
                             g_ghost_mode, g_has_tier3, has_awaiting_gps);
                }
            }
        EndDrawing();
    }

    for (int i = 0; i < g_source_count; i++) data_source_close(&g_sources[i]);
    g_source_count = 0;
    hud_cleanup(&g_hud);
    for (int i = 0; i < vehicle_inited_count; i++) vehicle_cleanup(&vehicles[i]);
    scene_cleanup(&scene);
    SetLoadFileTextCallback(NULL);
    SetLoadFileDataCallback(NULL);
    SetSaveFileDataCallback(NULL);
    SetSaveFileTextCallback(NULL);
    android_app->onInputEvent = orig_input_handler;
    CloseWindow();
    return 0;
}
