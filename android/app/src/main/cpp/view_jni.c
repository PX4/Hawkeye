#include "view_control.h"
#include <jni.h>

// Requests start at "none" (-1 for the mode requests, 0 for the toggle); snapshot fields
// are fine zero-initialized (cam=CHASE, ortho=NONE, panel hidden).
view_control_t g_view_control = {
    .req_cam_mode    = -1,
    .req_ortho_mode  = -1,
    .req_toggle_panel = 0,
};

int view_control_take_cam_mode(void) {
    return atomic_exchange(&g_view_control.req_cam_mode, -1);
}

int view_control_take_ortho_mode(void) {
    return atomic_exchange(&g_view_control.req_ortho_mode, -1);
}

int view_control_take_toggle_panel(void) {
    return atomic_exchange(&g_view_control.req_toggle_panel, 0);
}

int view_control_top_inset(void) {
    return atomic_load(&g_view_control.top_inset_px);
}

void view_control_publish(int cam_mode, int ortho_mode, int panel_visible) {
    atomic_store(&g_view_control.snap_cam_mode, cam_mode);
    atomic_store(&g_view_control.snap_ortho_mode, ortho_mode);
    atomic_store(&g_view_control.snap_panel_visible, panel_visible);
}

// --- JNI surface for com.px4.hawkeye.android.render.NativeViewController ---

// camMode: -1 none, else camera_mode_t (also exits ortho). orthoMode: -1 none, else
// ortho_mode_t. A view selection sends exactly one of the two; the other is -1.
JNIEXPORT void JNICALL
Java_com_px4_hawkeye_android_render_NativeViewController_nativeSetView(
        JNIEnv *env, jobject thiz, jint camMode, jint orthoMode) {
    (void)env; (void)thiz;
    atomic_store(&g_view_control.req_cam_mode, (int)camMode);
    atomic_store(&g_view_control.req_ortho_mode, (int)orthoMode);
}

JNIEXPORT void JNICALL
Java_com_px4_hawkeye_android_render_NativeViewController_nativeToggleSidePanel(
        JNIEnv *env, jobject thiz) {
    (void)env; (void)thiz;
    atomic_store(&g_view_control.req_toggle_panel, 1);
}

JNIEXPORT void JNICALL
Java_com_px4_hawkeye_android_render_NativeViewController_nativeSetTopInset(
        JNIEnv *env, jobject thiz, jint px) {
    (void)env; (void)thiz;
    atomic_store(&g_view_control.top_inset_px, (int)px);
}

// Returns [camMode, orthoMode, panelVisible].
JNIEXPORT jintArray JNICALL
Java_com_px4_hawkeye_android_render_NativeViewController_nativeGetView(
        JNIEnv *env, jobject thiz) {
    (void)thiz;
    jint values[3];
    values[0] = (jint)atomic_load(&g_view_control.snap_cam_mode);
    values[1] = (jint)atomic_load(&g_view_control.snap_ortho_mode);
    values[2] = (jint)atomic_load(&g_view_control.snap_panel_visible);

    jintArray array = (*env)->NewIntArray(env, 3);
    if (array != NULL) {
        (*env)->SetIntArrayRegion(env, array, 0, 3, values);
    }
    return array;
}
