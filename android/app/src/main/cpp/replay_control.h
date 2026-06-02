#ifndef REPLAY_CONTROL_H
#define REPLAY_CONTROL_H

#include <stdatomic.h>
#include <stdbool.h>

struct data_source;

/*
 * Lock-free bridge between the JVM main thread (JNI setters/getter, driven by the
 * Compose transport overlay) and raylib's render thread (which owns g_ds). The JVM
 * posts control *requests*; the render loop consumes them each frame via
 * replay_control_apply() and publishes a status *snapshot* via replay_control_publish().
 * The render thread is the only writer of g_ds.playback, so there are no data races.
 */
typedef struct {
    // Requests: written by the JVM, consumed (cleared) by the render thread.
    atomic_int    req_pause;      // -1 = no change, 0 = play, 1 = pause
    atomic_bool   req_speed_set;
    _Atomic float req_speed;
    atomic_bool   req_seek_set;
    _Atomic float req_seek_s;

    // Snapshot: written by the render thread, read by the JVM.
    atomic_bool   snap_active;
    atomic_bool   snap_paused;
    _Atomic float snap_pos_s;
    _Atomic float snap_dur_s;
    _Atomic float snap_speed;

    // Live MAVLink status snapshot: written by the render thread, read by the JVM.
    atomic_int    snap_live_state;   // 0 = waiting, 1 = connected, 2 = lost
    atomic_uint   snap_live_sysid;   // valid when state == connected
    atomic_uint   snap_live_port;    // bound listen port, echoed for the UI
} replay_control_t;

extern replay_control_t g_replay_control;

// Render-thread side, called from the android_main loop each frame.
void replay_control_apply(struct data_source *ds, bool active);
void replay_control_publish(struct data_source *ds, bool active);

// Publishes the live MAVLink connection snapshot (no-op outside live mode).
void live_status_publish(struct data_source *ds, bool active, bool live_mode, unsigned int port);

#endif
