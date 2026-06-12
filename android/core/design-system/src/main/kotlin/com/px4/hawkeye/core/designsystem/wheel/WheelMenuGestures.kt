package com.px4.hawkeye.core.designsystem.wheel

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Reference gesture producer for [WheelMenu] in hosts where Compose owns the touch
 * input: the wheel opens at the press point when the platform long-press timeout fires
 * with the pointer still within touch slop of where it went down; dragging then
 * highlights, releasing selects (invoking [onSelected] with the slice index), and a
 * second finger cancels without selecting, whether it lands before or after the wheel
 * opens.
 *
 * Opening is cancelled when the pointer lifts before the timeout, sits beyond touch
 * slop when the timeout fires (the explicit check below; [awaitLongPressOrCancellation]
 * itself never cancels on movement), or when another gesture handler consumes the
 * events, which is how pan/scroll hosts claim the gesture. While the wheel is open, a
 * tracked change that arrives already consumed (a system cancellation, or another
 * handler claiming the gesture) closes the wheel without selecting.
 *
 * [onSelected] is read through [rememberUpdatedState], so the most recently composed
 * callback is always the one invoked, even when the lambda changes mid-gesture.
 *
 * This is one producer, not the only one: hosts whose gestures are detected elsewhere
 * (e.g. a native input layer) skip this modifier and drive [WheelMenuState] directly.
 */
fun Modifier.wheelMenuGestures(
    state: WheelMenuState,
    onSelected: (Int) -> Unit,
): Modifier = composed {
    val currentOnSelected by rememberUpdatedState(onSelected)
    pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown()
            val hold = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
            // The long press must still be the finger that went down, alone. A retarget
            // to another pointer or a second finger during the pending phase belongs to
            // the host (pinch/pan), mirroring the C machine's PENDING -> REJECTED.
            if (hold.id != down.id || currentEvent.changes.count { it.pressed } > 1) {
                return@awaitEachGesture
            }
            // A pointer that has drifted past touch slop by the time the timeout fires is a
            // pan, not a wheel press. awaitLongPressOrCancellation does not check movement.
            if ((hold.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                return@awaitEachGesture
            }
            state.open(hold.position)
            state.move(hold.position)
            while (true) {
                val event = awaitPointerEvent()
                if (event.changes.count { it.pressed } > 1) {
                    state.cancel() // second finger: the rest of the gesture belongs to the host
                    break
                }
                val change = event.changes.firstOrNull { it.id == hold.id }
                if (change == null || change.isConsumed) {
                    // Tracked pointer gone, or its change arrived already consumed (a
                    // system cancel or another handler): close without selecting.
                    state.cancel()
                    break
                }
                change.consume()
                if (!change.pressed) {
                    state.release()?.let(currentOnSelected)
                    break
                }
                state.move(change.position)
            }
        }
    }
}
