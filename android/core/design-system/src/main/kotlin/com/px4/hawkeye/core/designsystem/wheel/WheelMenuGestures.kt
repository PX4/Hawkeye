package com.px4.hawkeye.core.designsystem.wheel

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Reference gesture producer for [WheelMenu] in hosts where Compose owns the touch
 * input: long-press (platform timeout + touch slop) opens the wheel at the press point,
 * dragging highlights, releasing selects (invoking [onSelected] with the slice index),
 * and a second finger cancels without selecting.
 *
 * This is one producer, not the only one: hosts whose gestures are detected elsewhere
 * (e.g. a native input layer) skip this modifier and drive [WheelMenuState] directly.
 */
fun Modifier.wheelMenuGestures(
    state: WheelMenuState,
    onSelected: (Int) -> Unit,
): Modifier = pointerInput(state) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val hold = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
        state.open(hold.position)
        state.move(hold.position)
        while (true) {
            val event = awaitPointerEvent()
            if (event.changes.count { it.pressed } > 1) {
                state.cancel() // second finger: the rest of the gesture belongs to the host
                break
            }
            val change = event.changes.firstOrNull { it.id == hold.id }
            if (change == null) {
                state.cancel()
                break
            }
            change.consume()
            if (!change.pressed) {
                state.release()?.let(onSelected)
                break
            }
            state.move(change.position)
        }
    }
}
