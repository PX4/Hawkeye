/*
 * wheel_gesture.h -- Tap-and-hold state machine for the radial wheel menu.
 *
 * Pure logic, zero Raylib dependency: the caller feeds it the touch count and
 * primary-touch position each frame, so the same code serves the Android
 * renderer and standalone unit tests (see tests/test_wheel_gesture.c).
 *
 * A single finger that stays within WHEEL_SLOP_PX for WHEEL_HOLD_S opens the
 * wheel; movement beyond the slop first is a camera orbit and the machine
 * stands down until all fingers lift. While open, the finger position drives
 * the highlight; lifting the finger publishes a one-shot release (selection),
 * and a second finger cancels without selecting.
 */
#ifndef WHEEL_GESTURE_H
#define WHEEL_GESTURE_H

#include <math.h>
#include <stdbool.h>

#ifndef WHEEL_HOLD_S
#define WHEEL_HOLD_S 0.45f
#endif

#ifndef WHEEL_SLOP_PX
#define WHEEL_SLOP_PX 24.0f
#endif

typedef enum {
    WHEEL_IDLE = 0,
    WHEEL_PENDING,  /* finger down, waiting out the hold threshold */
    WHEEL_OPEN,     /* wheel open, finger drives the highlight */
    WHEEL_REJECTED, /* camera owns this gesture; wait for all fingers up */
} wheel_phase_t;

typedef struct {
    wheel_phase_t phase;
    float center_x, center_y;   /* where the wheel opened */
    float finger_x, finger_y;   /* latest finger position while open */
    float hold_s;               /* time accumulated in PENDING */
    unsigned release_seq;       /* bumped once per release-while-open */
    float release_x, release_y; /* finger position at that release */
} wheel_gesture_t;

/*
 * Advance the machine one frame. touch_count and (x, y) describe the primary
 * touch; dt is the frame delta in seconds. Returns true while the wheel owns
 * the gesture and camera input must be suppressed.
 */
static inline bool wheel_gesture_update(wheel_gesture_t *g, int touch_count,
                                        float x, float y, float dt)
{
    switch (g->phase) {
    case WHEEL_IDLE:
        if (touch_count == 1) {
            g->phase = WHEEL_PENDING;
            g->center_x = x;
            g->center_y = y;
            g->finger_x = x;
            g->finger_y = y;
            g->hold_s = 0.0f;
        } else if (touch_count > 1) {
            g->phase = WHEEL_REJECTED;
        }
        break;

    case WHEEL_PENDING:
        if (touch_count == 0) {
            g->phase = WHEEL_IDLE; /* a tap: no wheel, no selection */
        } else if (touch_count > 1) {
            g->phase = WHEEL_REJECTED;
        } else if (hypotf(x - g->center_x, y - g->center_y) > WHEEL_SLOP_PX) {
            g->phase = WHEEL_REJECTED; /* an orbit, not a hold */
        } else {
            g->hold_s += dt;
            if (g->hold_s >= WHEEL_HOLD_S) g->phase = WHEEL_OPEN;
        }
        break;

    case WHEEL_OPEN:
        if (touch_count == 1) {
            g->finger_x = x;
            g->finger_y = y;
        } else if (touch_count == 0) {
            g->release_x = g->finger_x;
            g->release_y = g->finger_y;
            g->release_seq++;
            g->phase = WHEEL_IDLE;
        } else {
            g->phase = WHEEL_REJECTED; /* second finger cancels, no selection */
        }
        break;

    case WHEEL_REJECTED:
        if (touch_count == 0) g->phase = WHEEL_IDLE;
        break;
    }

    return g->phase == WHEEL_PENDING || g->phase == WHEEL_OPEN;
}

#endif
