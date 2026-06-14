#ifndef SWARM_CONTROL_H
#define SWARM_CONTROL_H

#include <stdatomic.h>
#include "wheel_gesture.h"

/*
 * Lock-free bridge between the JVM main thread (the Compose swarm-wheel overlay) and
 * raylib's render thread, following the replay_control.h pattern: the render thread is
 * the only writer of the snapshot fields and the only consumer of the request fields.
 *
 * The render loop owns the tap-and-hold detection (wheel_gesture.h fed from raylib's
 * touch state) and publishes the machine's phase plus the session shape every frame;
 * the overlay polls the snapshot, draws the wheel, resolves the hovered slice in
 * Kotlin, and posts the chosen drone index back through req_select.
 */
typedef struct {
    // Request: written by the JVM, consumed (reset to -1) by the render thread.
    atomic_int    req_select;        // -1 = none, else drone index to make current

    // Wheel gesture snapshot: written by the render thread, read by the JVM.
    atomic_int    snap_phase;        // wheel_phase_t as int
    _Atomic float snap_center_x;     // where the wheel opened (surface px)
    _Atomic float snap_center_y;
    _Atomic float snap_finger_x;     // latest finger position while open
    _Atomic float snap_finger_y;
    atomic_uint   snap_release_seq;  // bumped once per release-while-open
    _Atomic float snap_release_x;    // finger position at that release
    _Atomic float snap_release_y;

    // Session snapshot: written by the render thread, read by the JVM.
    atomic_int    snap_drone_count;  // 0 until a session loads
    atomic_int    snap_selected;     // index the camera/HUD currently follow
} swarm_control_t;

extern swarm_control_t g_swarm_control;

// Render-thread side, called from the android_main loop each frame.

// Returns the pending drone-selection request (and clears it), or -1 if none.
int swarm_control_take_select(void);

// Publishes the wheel gesture state and session shape for the overlay to poll.
void swarm_control_publish(const wheel_gesture_t *g, int drone_count, int selected);

#endif
