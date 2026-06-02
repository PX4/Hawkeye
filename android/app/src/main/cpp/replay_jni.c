#include "replay_control.h"
#include "data_source.h"
#include <jni.h>

// req_pause starts at -1 (no change); every other field is fine zero-initialized.
replay_control_t g_replay_control = { .req_pause = -1 };

void replay_control_apply(struct data_source *ds, bool active) {
    if (!active) return;

    int pause = atomic_exchange(&g_replay_control.req_pause, -1);
    if (pause == 0) ds->playback.paused = false;
    else if (pause == 1) ds->playback.paused = true;

    if (atomic_exchange(&g_replay_control.req_speed_set, false)) {
        ds->playback.speed = atomic_load(&g_replay_control.req_speed);
    }
    if (atomic_exchange(&g_replay_control.req_seek_set, false)) {
        data_source_seek(ds, atomic_load(&g_replay_control.req_seek_s));
    }
}

void replay_control_publish(struct data_source *ds, bool active) {
    atomic_store(&g_replay_control.snap_active, active);
    if (active) {
        atomic_store(&g_replay_control.snap_paused, ds->playback.paused);
        atomic_store(&g_replay_control.snap_pos_s, ds->playback.position_s);
        atomic_store(&g_replay_control.snap_dur_s, ds->playback.duration_s);
        atomic_store(&g_replay_control.snap_speed, ds->playback.speed);
    }
}

void live_status_publish(struct data_source *ds, bool active, bool live_mode, unsigned int port) {
    if (!live_mode) return;
    atomic_store(&g_replay_control.snap_live_port, port);

    // Render-thread-only latch: distinguishes "never connected" (waiting) from
    // "was connected, telemetry stopped" (lost). ds->connected is the receiver's own
    // heartbeat-timeout flag (see mavlink_receiver.c).
    static bool s_ever_connected = false;
    int state;
    if (active && ds->connected) {
        s_ever_connected = true;
        state = 1;  // connected
        atomic_store(&g_replay_control.snap_live_sysid, ds->sysid);
    } else if (s_ever_connected) {
        state = 2;  // lost
    } else {
        state = 0;  // waiting
    }
    atomic_store(&g_replay_control.snap_live_state, state);
}

// --- JNI surface for com.px4.hawkeye.android.render.NativeReplayController ---

JNIEXPORT void JNICALL
Java_com_px4_hawkeye_android_render_NativeReplayController_nativeSetPaused(
        JNIEnv *env, jobject thiz, jboolean paused) {
    (void)env; (void)thiz;
    atomic_store(&g_replay_control.req_pause, paused ? 1 : 0);
}

JNIEXPORT void JNICALL
Java_com_px4_hawkeye_android_render_NativeReplayController_nativeSetSpeed(
        JNIEnv *env, jobject thiz, jfloat speed) {
    (void)env; (void)thiz;
    atomic_store(&g_replay_control.req_speed, speed);
    atomic_store(&g_replay_control.req_speed_set, true);
}

JNIEXPORT void JNICALL
Java_com_px4_hawkeye_android_render_NativeReplayController_nativeSeekTo(
        JNIEnv *env, jobject thiz, jfloat seconds) {
    (void)env; (void)thiz;
    atomic_store(&g_replay_control.req_seek_s, seconds);
    atomic_store(&g_replay_control.req_seek_set, true);
}

// Returns [active, paused, positionS, durationS, speed] (bools as 0/1).
JNIEXPORT jfloatArray JNICALL
Java_com_px4_hawkeye_android_render_NativeReplayController_nativeGetStatus(
        JNIEnv *env, jobject thiz) {
    (void)thiz;
    jfloat values[5];
    values[0] = atomic_load(&g_replay_control.snap_active) ? 1.0f : 0.0f;
    values[1] = atomic_load(&g_replay_control.snap_paused) ? 1.0f : 0.0f;
    values[2] = atomic_load(&g_replay_control.snap_pos_s);
    values[3] = atomic_load(&g_replay_control.snap_dur_s);
    values[4] = atomic_load(&g_replay_control.snap_speed);

    jfloatArray array = (*env)->NewFloatArray(env, 5);
    if (array != NULL) {
        (*env)->SetFloatArrayRegion(env, array, 0, 5, values);
    }
    return array;
}

// --- JNI surface for com.px4.hawkeye.android.render.NativeLiveStatusController ---

// Returns [state, sysid, port]; state 0 = waiting, 1 = connected, 2 = lost.
JNIEXPORT jfloatArray JNICALL
Java_com_px4_hawkeye_android_render_NativeLiveStatusController_nativeGetLiveStatus(
        JNIEnv *env, jobject thiz) {
    (void)thiz;
    jfloat values[3];
    values[0] = (jfloat)atomic_load(&g_replay_control.snap_live_state);
    values[1] = (jfloat)atomic_load(&g_replay_control.snap_live_sysid);
    values[2] = (jfloat)atomic_load(&g_replay_control.snap_live_port);

    jfloatArray array = (*env)->NewFloatArray(env, 3);
    if (array != NULL) {
        (*env)->SetFloatArrayRegion(env, array, 0, 3, values);
    }
    return array;
}
