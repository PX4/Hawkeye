#include "raylib.h"
#include "raymath.h"
#include "asset_path.h"
#include "scene.h"
#include "vehicle.h"
#include "data_source.h"
#include "hud.h"
#include "replay_control.h"
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
#define MAX_PATH_LEN ASSET_MAX_PATH

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

// The library repository (Kotlin) stages a .ulg via .tmp + atomic rename to
// inbox/current.ulg, then writes a fresh millis token into inbox/.ready. We poll
// the sentinel's *contents* (not stat-mtime — f2fs has 1-second granularity and
// would coalesce two stages in the same wall second) once per second and reload
// the replay when the token changes.
static data_source_t g_ds;
static bool          g_ds_active = false;
static long long     g_last_ready_token = 0;
static hud_t         g_hud;
// Initialized to a large negative value so the first poll always proceeds.
static double        g_last_poll_time = -1e9;

// For camera-follow: each frame we translate orbit_target and camera.position
// by (vehicle.position - g_last_vehicle_pos). Initialized to vehicle.position
// at startup, and re-seeded on every reload so the first delta is zero.
static Vector3       g_last_vehicle_pos = {0};

// Returns the millis token in the sentinel file, or 0 on missing/empty/parse-fail.
static long long read_ready_token(const char *path) {
    FILE *f = fopen(path, "rb");
    if (!f) return 0;
    char buf[64];
    size_t n = fread(buf, 1, sizeof(buf) - 1, f);
    fclose(f);
    if (n == 0) return 0;
    buf[n] = '\0';
    char *end = NULL;
    long long val = strtoll(buf, &end, 10);
    return (end == buf) ? 0 : val;
}

static int try_load_inbox_ulog(vehicle_t *vehicle) {
    // Throttle to 1 Hz. Sentinel changes are user-driven (library playback), so
    // a per-frame stat()/read() at 60 Hz is wasted work — and since we read
    // the sentinel's contents (not its mtime), an up-to-1-second detection
    // delay is the only cost.
    double now = GetTime();
    if (now - g_last_poll_time < 1.0) return 0;
    g_last_poll_time = now;

    char ready[MAX_PATH_LEN];
    snprintf(ready, sizeof(ready), "%s/inbox/.ready", s_internal_data_path);
    long long token = read_ready_token(ready);
    if (token == 0 || token == g_last_ready_token) return 0;

    // Parse-then-swap: try to construct the new data source first. If it
    // fails (corrupt file, partial copy, etc.) the previous replay keeps
    // playing — we don't tear down working state on speculation.
    char ulg[MAX_PATH_LEN];
    snprintf(ulg, sizeof(ulg), "%s/inbox/current.ulg", s_internal_data_path);
    data_source_t new_ds = {0};
    int rc = data_source_ulog_create(&new_ds, ulg);
    if (rc != 0) {
        __android_log_print(ANDROID_LOG_ERROR, "Hawkeye",
            "data_source_ulog_create(%s) failed: %d", ulg, rc);
        // Mark the bad token consumed so we don't retry every second; the
        // user must re-open the log to trigger another attempt.
        g_last_ready_token = token;
        return 0;
    }

    if (g_ds_active) {
        data_source_close(&g_ds);
        vehicle_reset_trail(vehicle);
    }
    g_ds = new_ds;
    g_ds_active = true;
    g_last_ready_token = token;

    // Reset origin tracking so a new replay without a valid home falls back to
    // vehicle_update's wait-and-latch logic instead of inheriting the previous
    // replay's origin (lat0/lon0/alt0 would otherwise stay set from the old log).
    vehicle->origin_set = false;
    vehicle->origin_wait_count = 0;

    // Pre-seed origin from the parsed home so positions are computed relative
    // to home. vehicle_update's own first-sample origin-init latches onto
    // whatever state it sees first — if state.lat is briefly 0 (drone powered
    // on, no GPS lock yet), it sets lat0=0 and the rest of the flight renders
    // at absolute lat/lon coordinates millions of meters from origin. The ULog
    // pre-scan populates ctx->home reliably, so use it when available.
    if (g_ds.home.valid) {
        vehicle->lat0 = g_ds.home.lat * 1e-7 * (M_PI / 180.0);
        vehicle->lon0 = g_ds.home.lon * 1e-7 * (M_PI / 180.0);
        vehicle->alt0 = g_ds.home.alt * 1e-3;
        vehicle->origin_set = true;
    }

    // Re-seed the follow baseline so the first frame's delta covers the jump
    // from the previous replay's last position (or the dummy startup position)
    // to this replay's first valid sample — keeps the new drone in view.
    g_last_vehicle_pos = vehicle->position;

    __android_log_print(ANDROID_LOG_INFO, "Hawkeye",
        "loaded ulg: duration=%.1fs token=%lld", g_ds.playback.duration_s, token);
    return 1;
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

    vehicle_t vehicle = {0};
    vehicle_init(&vehicle, MODEL_QUADROTOR, scene.lighting_shader);

    hud_init(&g_hud);
    // The transport bar is driven by a Compose overlay on Android, so suppress the
    // native one (keeps the rest of the HUD: instruments, telemetry, annunciators).
    g_hud.show_transport = false;
    // Bump HUD scale on mobile so values/labels meet M3 readability floors
    // (~42 px body, ~33 px label at this DPI). Desktop/WASM leave this at 1.0.
    g_hud.scale_mul = 1.5f;

    // Position vehicle slightly above origin so it's visible with the default camera
    vehicle.position = (Vector3){ 0.0f, 0.5f, 0.0f };

    scene.cam_mode   = CAM_MODE_FREE;
    scene.free_track = true;

    Vector3 orbit_target    = vehicle.position;
    Vector2 prev_touch      = {0};
    float   prev_pinch_dist = 0.0f;
    Vector2 prev_mid        = {0};
    int     prev_count      = 0;

    scene.camera.target = orbit_target;
    g_last_vehicle_pos  = vehicle.position;

    // Pick up a .ulg already staged into the inbox before native main() ran.
    try_load_inbox_ulog(&vehicle);

    while (!WindowShouldClose()) {
        // Catch a new log staged while the renderer is already running.
        try_load_inbox_ulog(&vehicle);

        // Apply pending transport requests from the Compose overlay (JVM thread).
        replay_control_apply(&g_ds, g_ds_active);

        if (g_ds_active) {
            data_source_poll(&g_ds, GetFrameTime());
            vehicle_update(&vehicle, &g_ds.state, &g_ds.home);

            // Camera follow: translate the orbit center and the camera by the
            // vehicle's frame-to-frame motion so the user's orbit/pan offset
            // (relative to the vehicle) stays constant.
            if (g_ds.state.valid) {
                Vector3 delta = Vector3Subtract(vehicle.position, g_last_vehicle_pos);
                orbit_target = Vector3Add(orbit_target, delta);
                scene.camera.position = Vector3Add(scene.camera.position, delta);
                scene.camera.target = orbit_target;
                g_last_vehicle_pos = vehicle.position;
            }
        }

        // Publish playback status for the Compose overlay to read via JNI.
        replay_control_publish(&g_ds, g_ds_active);

        handle_touch(&scene.camera, &orbit_target,
                     &prev_count, &prev_touch,
                     &prev_pinch_dist, &prev_mid);

        hud_update(&g_hud,
                   g_ds_active ? g_ds.state.time_usec : 0,
                   g_ds_active ? g_ds.connected : false,
                   GetFrameTime());

        BeginDrawing();
            scene_draw_sky(&scene);
            BeginMode3D(scene.camera);
                scene_draw(&scene);
                vehicle_draw(&vehicle, scene.theme,
                             /*selected=*/true,
                             /*trail_mode=*/g_ds_active ? 1 : 0,
                             /*show_ground_track=*/false,
                             /*cam_pos=*/scene.camera.position,
                             /*classic_colors=*/false);
            EndMode3D();

            // HUD overlay. When no .ulg is loaded, hand hud_draw a zeroed
            // data_source so the status row reads "Waiting…" without crashing.
            {
                data_source_t empty_src = {0};
                const data_source_t *src_ptr = g_ds_active ? &g_ds : &empty_src;
                hud_marker_data_t user_md = {0};
                hud_marker_data_t sys_md  = {0};
                bool has_awaiting_gps = g_ds_active && !vehicle.origin_set && g_ds.home.valid;
                if (g_hud.mode == HUD_CONSOLE) {
                    hud_draw(&g_hud, &vehicle, src_ptr, /*vehicle_count=*/1, /*selected=*/0,
                             GetScreenWidth(), GetScreenHeight(),
                             scene.theme, /*trail_mode=*/g_ds_active ? 1 : 0,
                             &user_md, &sys_md, /*marker_vehicle_count=*/1,
                             /*ghost_mode=*/false, /*has_tier3=*/false, has_awaiting_gps);
                }
            }
        EndDrawing();
    }

    if (g_ds_active) {
        data_source_close(&g_ds);
        g_ds_active = false;
    }
    hud_cleanup(&g_hud);
    vehicle_cleanup(&vehicle);
    scene_cleanup(&scene);
    SetLoadFileTextCallback(NULL);
    SetLoadFileDataCallback(NULL);
    SetSaveFileDataCallback(NULL);
    SetSaveFileTextCallback(NULL);
    android_app->onInputEvent = orig_input_handler;
    CloseWindow();
    return 0;
}
