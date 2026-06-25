#ifndef VIEW_CONTROL_H
#define VIEW_CONTROL_H

#include <stdatomic.h>

/*
 * Lock-free bridge for camera/view switching between the JVM main thread (the Compose
 * wheel overlay) and raylib's render thread, following the swarm_control.h pattern: the
 * render thread is the only consumer of the request fields and the only writer of the
 * snapshot fields.
 *
 * Requests use -1 as "no request". A perspective-camera request (camera_mode_t) also
 * exits any fullscreen ortho view; an ortho request (ortho_mode_t 1..6) switches into
 * one. The snapshot publishes the active view so the overlay can mark the current slice.
 */
typedef struct {
    // Requests: written by the JVM, consumed (reset) by the render thread.
    atomic_int req_cam_mode;      // -1 none, else camera_mode_t (sets perspective, exits ortho)
    atomic_int req_ortho_mode;    // -1 none, else ortho_mode_t  (1..6 switch into a fullscreen ortho)
    atomic_int req_toggle_panel;  // 0 none, 1 = toggle the 3-up ortho side panel
    atomic_int top_inset_px;      // px reserved at the top (the Compose media bar); 0 = none

    // Snapshot: written by the render thread, read by the JVM.
    atomic_int snap_cam_mode;     // current camera_mode_t
    atomic_int snap_ortho_mode;   // current ortho_mode_t (0 = perspective)
    atomic_int snap_panel_visible;// 1 if the 3-up ortho side panel is showing
} view_control_t;

extern view_control_t g_view_control;

// Render-thread side, called from the android_main loop each frame.

// Returns the pending camera-mode request (and clears it), or -1 if none.
int view_control_take_cam_mode(void);

// Returns the pending ortho-mode request (and clears it), or -1 if none.
int view_control_take_ortho_mode(void);

// Returns 1 if a side-panel toggle is pending (and clears it), else 0.
int view_control_take_toggle_panel(void);

// Px reserved at the top of the screen for the Compose media bar (0 if none). Persistent
// (not cleared on read); the JVM updates it whenever the overlay's height changes.
int view_control_top_inset(void);

// Publishes the active view for the overlay to poll.
void view_control_publish(int cam_mode, int ortho_mode, int panel_visible);

#endif
