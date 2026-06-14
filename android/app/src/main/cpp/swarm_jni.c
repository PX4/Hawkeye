#include "swarm_control.h"
#include <jni.h>

// req_select starts at -1 (no request); every other field is fine zero-initialized.
swarm_control_t g_swarm_control = { .req_select = -1 };

int swarm_control_take_select(void) {
    return atomic_exchange(&g_swarm_control.req_select, -1);
}

void swarm_control_publish(const wheel_gesture_t *g, int drone_count, int selected) {
    atomic_store(&g_swarm_control.snap_phase, (int)g->phase);
    atomic_store(&g_swarm_control.snap_center_x, g->center_x);
    atomic_store(&g_swarm_control.snap_center_y, g->center_y);
    atomic_store(&g_swarm_control.snap_finger_x, g->finger_x);
    atomic_store(&g_swarm_control.snap_finger_y, g->finger_y);
    atomic_store(&g_swarm_control.snap_release_x, g->release_x);
    atomic_store(&g_swarm_control.snap_release_y, g->release_y);
    // release_seq last: a JVM poll that sees the new seq is guaranteed to also see the
    // matching release position written above.
    atomic_store(&g_swarm_control.snap_release_seq, g->release_seq);
    atomic_store(&g_swarm_control.snap_drone_count, drone_count);
    atomic_store(&g_swarm_control.snap_selected, selected);
}

// --- JNI surface for com.px4.hawkeye.android.render.NativeSwarmController ---

// Returns [phase, centerX, centerY, fingerX, fingerY, releaseSeq, releaseX, releaseY,
//          droneCount, selected].
JNIEXPORT jfloatArray JNICALL
Java_com_px4_hawkeye_android_render_NativeSwarmController_nativeGetWheel(
        JNIEnv *env, jobject thiz) {
    (void)thiz;
    jfloat values[10];
    values[0] = (jfloat)atomic_load(&g_swarm_control.snap_phase);
    values[1] = atomic_load(&g_swarm_control.snap_center_x);
    values[2] = atomic_load(&g_swarm_control.snap_center_y);
    values[3] = atomic_load(&g_swarm_control.snap_finger_x);
    values[4] = atomic_load(&g_swarm_control.snap_finger_y);
    values[5] = (jfloat)atomic_load(&g_swarm_control.snap_release_seq);
    values[6] = atomic_load(&g_swarm_control.snap_release_x);
    values[7] = atomic_load(&g_swarm_control.snap_release_y);
    values[8] = (jfloat)atomic_load(&g_swarm_control.snap_drone_count);
    values[9] = (jfloat)atomic_load(&g_swarm_control.snap_selected);

    jfloatArray array = (*env)->NewFloatArray(env, 10);
    if (array != NULL) {
        (*env)->SetFloatArrayRegion(env, array, 0, 10, values);
    }
    return array;
}

JNIEXPORT void JNICALL
Java_com_px4_hawkeye_android_render_NativeSwarmController_nativeSelectDrone(
        JNIEnv *env, jobject thiz, jint index) {
    (void)env; (void)thiz;
    atomic_store(&g_swarm_control.req_select, (int)index);
}
