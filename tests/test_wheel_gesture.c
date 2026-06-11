/*
 * Unit tests for the tap-and-hold wheel-menu gesture state machine in
 * wheel_gesture.h. No Raylib dependency -- compiles standalone with libc.
 */
#include "wheel_gesture.h"
#include <assert.h>
#include <stdio.h>

static const float FRAME = 1.0f / 60.0f;

/* Advance `frames` frames with one finger resting at (x, y). */
static void hold_frames(wheel_gesture_t *g, int frames, float x, float y)
{
    for (int i = 0; i < frames; i++)
        wheel_gesture_update(g, 1, x, y, FRAME);
}

/* Frames needed to cross the hold threshold, plus margin. */
static int frames_to_open(void)
{
    return (int)(WHEEL_HOLD_S / FRAME) + 2;
}

static void test_quick_tap_does_not_open(void)
{
    wheel_gesture_t g = {0};
    hold_frames(&g, 5, 100.0f, 100.0f);
    wheel_gesture_update(&g, 0, 0.0f, 0.0f, FRAME);
    assert(g.phase == WHEEL_IDLE);
    assert(g.release_seq == 0);
    printf("  PASS quick_tap_does_not_open\n");
}

static void test_hold_opens_after_threshold(void)
{
    wheel_gesture_t g = {0};
    hold_frames(&g, frames_to_open(), 100.0f, 100.0f);
    assert(g.phase == WHEEL_OPEN);
    assert(g.center_x == 100.0f && g.center_y == 100.0f);
    printf("  PASS hold_opens_after_threshold\n");
}

static void test_pending_suppresses_camera(void)
{
    wheel_gesture_t g = {0};
    bool owns = wheel_gesture_update(&g, 1, 100.0f, 100.0f, FRAME);
    assert(g.phase == WHEEL_PENDING);
    assert(owns);
    printf("  PASS pending_suppresses_camera\n");
}

static void test_drag_beyond_slop_hands_gesture_to_camera(void)
{
    wheel_gesture_t g = {0};
    hold_frames(&g, 3, 100.0f, 100.0f);
    bool owns = wheel_gesture_update(&g, 1, 100.0f + WHEEL_SLOP_PX + 1.0f, 100.0f, FRAME);
    assert(g.phase == WHEEL_REJECTED);
    assert(!owns);
    printf("  PASS drag_beyond_slop_hands_gesture_to_camera\n");
}

static void test_rejected_stays_until_all_fingers_up(void)
{
    wheel_gesture_t g = {0};
    hold_frames(&g, 3, 100.0f, 100.0f);
    wheel_gesture_update(&g, 1, 200.0f, 100.0f, FRAME); /* slop exceeded */
    assert(g.phase == WHEEL_REJECTED);
    /* Still rejected while the finger stays down, even back at the start point. */
    wheel_gesture_update(&g, 1, 100.0f, 100.0f, FRAME);
    assert(g.phase == WHEEL_REJECTED);
    wheel_gesture_update(&g, 0, 0.0f, 0.0f, FRAME);
    assert(g.phase == WHEEL_IDLE);
    printf("  PASS rejected_stays_until_all_fingers_up\n");
}

static void test_open_tracks_finger(void)
{
    wheel_gesture_t g = {0};
    hold_frames(&g, frames_to_open(), 100.0f, 100.0f);
    wheel_gesture_update(&g, 1, 140.0f, 160.0f, FRAME);
    assert(g.phase == WHEEL_OPEN);
    assert(g.finger_x == 140.0f && g.finger_y == 160.0f);
    /* Moving past the slop is fine once open; only PENDING has a slop. */
    wheel_gesture_update(&g, 1, 400.0f, 100.0f, FRAME);
    assert(g.phase == WHEEL_OPEN);
    printf("  PASS open_tracks_finger\n");
}

static void test_release_latches_position_and_bumps_seq(void)
{
    wheel_gesture_t g = {0};
    hold_frames(&g, frames_to_open(), 100.0f, 100.0f);
    wheel_gesture_update(&g, 1, 140.0f, 160.0f, FRAME);
    bool owns = wheel_gesture_update(&g, 0, 0.0f, 0.0f, FRAME);
    assert(g.phase == WHEEL_IDLE);
    assert(!owns);
    assert(g.release_seq == 1);
    assert(g.release_x == 140.0f && g.release_y == 160.0f);
    printf("  PASS release_latches_position_and_bumps_seq\n");
}

static void test_second_finger_cancels_without_selection(void)
{
    wheel_gesture_t g = {0};
    hold_frames(&g, frames_to_open(), 100.0f, 100.0f);
    wheel_gesture_update(&g, 2, 100.0f, 100.0f, FRAME);
    assert(g.phase == WHEEL_REJECTED);
    assert(g.release_seq == 0);
    printf("  PASS second_finger_cancels_without_selection\n");
}

static void test_pinch_from_idle_never_pends(void)
{
    wheel_gesture_t g = {0};
    bool owns = wheel_gesture_update(&g, 2, 100.0f, 100.0f, FRAME);
    assert(g.phase == WHEEL_REJECTED);
    assert(!owns);
    printf("  PASS pinch_from_idle_never_pends\n");
}

static void test_second_finger_during_pending_rejects(void)
{
    wheel_gesture_t g = {0};
    hold_frames(&g, 3, 100.0f, 100.0f);
    assert(g.phase == WHEEL_PENDING);
    wheel_gesture_update(&g, 2, 100.0f, 100.0f, FRAME);
    assert(g.phase == WHEEL_REJECTED);
    assert(g.release_seq == 0);
    printf("  PASS second_finger_during_pending_rejects\n");
}

static void test_exact_slop_stays_pending(void)
{
    wheel_gesture_t g = {0};
    hold_frames(&g, 3, 100.0f, 100.0f);
    /* The slop check is a strict >: moving by exactly WHEEL_SLOP_PX is a hold. */
    wheel_gesture_update(&g, 1, 100.0f + WHEEL_SLOP_PX, 100.0f, FRAME);
    assert(g.phase == WHEEL_PENDING);
    printf("  PASS exact_slop_stays_pending\n");
}

static void test_second_gesture_cycle(void)
{
    wheel_gesture_t g = {0};
    /* First cycle: open at (100, 100), release at (140, 160). */
    hold_frames(&g, frames_to_open(), 100.0f, 100.0f);
    wheel_gesture_update(&g, 1, 140.0f, 160.0f, FRAME);
    wheel_gesture_update(&g, 0, 0.0f, 0.0f, FRAME);
    assert(g.phase == WHEEL_IDLE);
    assert(g.release_seq == 1);
    /* Second cycle on the same struct: a fresh hold elsewhere reopens cleanly. */
    hold_frames(&g, frames_to_open(), 300.0f, 200.0f);
    assert(g.phase == WHEEL_OPEN);
    assert(g.center_x == 300.0f && g.center_y == 200.0f);
    wheel_gesture_update(&g, 1, 320.0f, 260.0f, FRAME);
    wheel_gesture_update(&g, 0, 0.0f, 0.0f, FRAME);
    assert(g.phase == WHEEL_IDLE);
    assert(g.release_seq == 2);
    assert(g.release_x == 320.0f && g.release_y == 260.0f);
    printf("  PASS second_gesture_cycle\n");
}

int main(void)
{
    printf("wheel_gesture tests:\n");
    test_quick_tap_does_not_open();
    test_hold_opens_after_threshold();
    test_pending_suppresses_camera();
    test_drag_beyond_slop_hands_gesture_to_camera();
    test_rejected_stays_until_all_fingers_up();
    test_open_tracks_finger();
    test_release_latches_position_and_bumps_seq();
    test_second_finger_cancels_without_selection();
    test_pinch_from_idle_never_pends();
    test_second_finger_during_pending_rejects();
    test_exact_slop_stays_pending();
    test_second_gesture_cycle();
    printf("All wheel_gesture tests passed.\n");
    return 0;
}
