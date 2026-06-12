package com.px4.hawkeye.android.render.swarm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.px4.hawkeye.android.R
import com.px4.hawkeye.android.render.SwarmWheelSnapshot
import com.px4.hawkeye.core.designsystem.HawkeyeDronePalette
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.core.designsystem.wheel.WheelMenu
import com.px4.hawkeye.core.designsystem.wheel.WheelMenuItem
import com.px4.hawkeye.core.designsystem.wheel.WheelMenuState
import com.px4.hawkeye.core.designsystem.wheel.rememberWheelMenuState
import com.px4.hawkeye.core.presentation.UiText
import com.px4.hawkeye.core.presentation.asString

/**
 * Hosted in a dedicated full-screen, non-touchable WindowManager panel above the renderer
 * (see HawkeyeActivity): the native engine owns the tap-and-hold gesture, this overlay
 * only draws the wheel and resolves the released slice.
 */
@Composable
fun SwarmWheelRoot(viewModel: SwarmWheelViewModel) {
    // The ViewModel owns the native-snapshot polling loop; the Root just collects state.
    val state by viewModel.state.collectAsStateWithLifecycle()
    SwarmWheelScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun SwarmWheelScreen(
    state: SwarmWheelState,
    onAction: (SwarmWheelAction) -> Unit,
) {
    // The wheel exists to switch between drones; single-drone (and empty) sessions keep
    // the overlay fully inert, matching the native side never arming the gesture.
    if (state.items.size < 2) return

    val resolvedItems = state.items.map {
        WheelMenuItem(
            label = it.label.asString(),
            accentColor = it.accentColor,
            hubLabel = it.hubLabel.asString(),
        )
    }
    val wheelState = rememberWheelMenuState(resolvedItems)
    // Guarded swap, applied after composition (snapshot writes don't belong in the
    // composition phase): assigning items while open clears the hover, so only push a
    // real session change, not every recomposition's structurally-equal copy.
    SideEffect {
        if (wheelState.items != resolvedItems) wheelState.items = resolvedItems
    }

    ProjectGestureIntoWheel(state.gesture, wheelState, onAction)

    WheelMenu(
        state = wheelState,
        selectHint = stringResource(R.string.swarm_wheel_hint),
    )
}

/**
 * Projects the polled native gesture snapshot into [WheelMenuState] — the "producer" the
 * widget's contract asks for, with `wheel_gesture.h` as the source instead of a Compose
 * pointerInput. Releases are processed before the phase branch on purpose: a release
 * arrives with the native phase already back at IDLE, and handling the phase first would
 * cancel the wheel and drop the selection.
 */
@Composable
private fun ProjectGestureIntoWheel(
    gesture: SwarmWheelSnapshot,
    wheelState: WheelMenuState,
    onAction: (SwarmWheelAction) -> Unit,
) {
    // Seed with the first-seen seq so an overlay (re)attached mid-session never replays
    // a release that happened before it existed.
    var handledReleaseSeq by remember { mutableIntStateOf(gesture.releaseSeq) }

    LaunchedEffect(gesture) {
        if (gesture.releaseSeq != handledReleaseSeq) {
            handledReleaseSeq = gesture.releaseSeq
            if (wheelState.isOpen) {
                wheelState.move(Offset(gesture.releaseX, gesture.releaseY))
                wheelState.release()?.let { onAction(SwarmWheelAction.OnDroneSelected(it)) }
            }
            // The native publish is not one atomic block, so this snapshot's phase can be
            // a stale OPEN paired with the fresh seq. The release consumed the gesture;
            // letting the phase branch run would reopen the wheel for one poll cycle.
            return@LaunchedEffect
        }
        when (gesture.phase) {
            SwarmWheelSnapshot.PHASE_OPEN -> {
                if (!wheelState.isOpen) {
                    wheelState.open(Offset(gesture.centerX, gesture.centerY))
                }
                wheelState.move(Offset(gesture.fingerX, gesture.fingerY))
            }

            else -> if (wheelState.isOpen) wheelState.cancel()
        }
    }
}

// Static previews render the closed (empty) state — the wheel opens only through the
// native gesture projection. The open-wheel visuals are previewed in the design-system
// WheelMenu previews.
@Preview(showBackground = true, widthDp = 400, heightDp = 400)
@Composable
private fun SwarmWheelScreenPreview() {
    HawkeyeTheme {
        SwarmWheelScreen(
            state = SwarmWheelState(
                items = listOf(
                    DroneWheelItemUi(
                        label = UiText.DynamicString("Drone 1"),
                        hubLabel = UiText.DynamicString("alpha.ulg"),
                        accentColor = HawkeyeDronePalette.colors[0],
                    ),
                    DroneWheelItemUi(
                        label = UiText.DynamicString("Drone 2"),
                        hubLabel = UiText.DynamicString("bravo.ulg"),
                        accentColor = HawkeyeDronePalette.colors[1],
                    ),
                ),
                gesture = SwarmWheelSnapshot.Idle.copy(droneCount = 2),
            ),
            onAction = {},
        )
    }
}
