package com.px4.hawkeye.android.render.wheel

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.px4.hawkeye.android.R
import com.px4.hawkeye.android.render.SwarmWheelSnapshot
import com.px4.hawkeye.core.designsystem.HawkeyeDronePalette
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.core.designsystem.wheel.WheelMenu
import com.px4.hawkeye.core.designsystem.wheel.WheelMenuItem
import com.px4.hawkeye.core.designsystem.wheel.WheelMenuNavigator
import com.px4.hawkeye.core.designsystem.wheel.WheelMenuState
import com.px4.hawkeye.core.designsystem.wheel.rememberWheelMenuNavigator
import com.px4.hawkeye.core.designsystem.wheel.rememberWheelMenuState
import com.px4.hawkeye.core.presentation.UiText
import com.px4.hawkeye.core.presentation.asString
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * Hosted in a dedicated full-screen, non-touchable WindowManager panel above the renderer
 * (see HawkeyeActivity): the native engine owns the tap-and-hold gesture, this overlay only
 * draws the wheel, drives nested navigation, and resolves the released slice. Nesting (dwell
 * to drill in, drag to the hub to go back) is pure Compose state on top of the native
 * gesture stream — the native machine just keeps the wheel open while a finger is down.
 */
@Composable
fun RootWheelRoot(viewModel: RootWheelViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RootWheelScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun RootWheelScreen(
    state: RootWheelState,
    onAction: (RootWheelAction) -> Unit,
) {
    val context = LocalContext.current
    // Resolve the UiText tree to plain-String WheelMenuItems once per menu change. asString
    // here is the non-composable Context overload, so the recursion stays out of composition.
    val rootItems = remember(state.root) { state.root.map { it.toMenuItem(context) } }

    val wheelState = rememberWheelMenuState(rootItems)
    val navigator = rememberWheelMenuNavigator(wheelState, rootItems)
    // Push menu changes (drone count, active-view highlight) into the navigator. Only takes
    // effect at the root, so it never yanks the user out of an open submenu.
    SideEffect {
        if (navigator.root != rootItems) navigator.root = rootItems
    }

    ProjectGestureIntoWheel(state.gesture, wheelState, navigator, onAction)
    DwellNavigation(wheelState, navigator)

    WheelMenu(
        state = wheelState,
        selectHint = stringResource(R.string.swarm_wheel_hint),
    )
}

/** Recursively resolves an app node to a design-system item (plain Strings, submenu intact). */
private fun WheelNodeUi.toMenuItem(context: Context): WheelMenuItem = WheelMenuItem(
    label = label.asString(context),
    accentColor = accentColor,
    hubLabel = hubLabel?.asString(context),
    id = id,
    isActive = isActive,
    children = children?.map { it.toMenuItem(context) },
)

/**
 * Projects the polled native gesture snapshot into [WheelMenuState] + [WheelMenuNavigator].
 * Releases are processed before the phase branch on purpose: a release arrives with the
 * native phase already back at IDLE, and handling the phase first would cancel the wheel and
 * drop the selection. On release a leaf dispatches its id; a parent (no children consumed via
 * dwell) or a hub release just closes. The navigator resets on every close so the next
 * gesture starts at the root.
 */
@Composable
private fun ProjectGestureIntoWheel(
    gesture: SwarmWheelSnapshot,
    wheelState: WheelMenuState,
    navigator: WheelMenuNavigator,
    onAction: (RootWheelAction) -> Unit,
) {
    // Seed with the first-seen seq so an overlay (re)attached mid-session never replays a
    // release that happened before it existed.
    var handledReleaseSeq by remember { mutableIntStateOf(gesture.releaseSeq) }

    LaunchedEffect(gesture) {
        if (gesture.releaseSeq != handledReleaseSeq) {
            handledReleaseSeq = gesture.releaseSeq
            if (wheelState.isOpen) {
                wheelState.move(Offset(gesture.releaseX, gesture.releaseY))
                val index = wheelState.release()
                val node = index?.let { navigator.current.getOrNull(it) }
                // Only leaves dispatch; parents are reached by dwell, not release.
                val leafId = node?.takeIf { it.children == null }?.id
                if (leafId != null) {
                    onAction(RootWheelAction.OnNodeSelected(leafId))
                }
            }
            navigator.reset()
            // The native publish is not one atomic block, so this snapshot's phase can be a
            // stale OPEN paired with the fresh seq. The release consumed the gesture; letting
            // the phase branch run would reopen the wheel for one poll cycle.
            return@LaunchedEffect
        }
        when (gesture.phase) {
            SwarmWheelSnapshot.PHASE_OPEN -> {
                if (!wheelState.isOpen) {
                    navigator.reset()
                    wheelState.open(Offset(gesture.centerX, gesture.centerY))
                }
                wheelState.move(Offset(gesture.fingerX, gesture.fingerY))
            }

            else -> if (wheelState.isOpen) {
                wheelState.cancel()
                navigator.reset()
            }
        }
    }
}

/**
 * Drives nested navigation from the live hover: dwelling on a parent slice drills into its
 * submenu. collectLatest cancels the pending delay the instant the hover changes, so only a
 * deliberate pause triggers a drill. There is no go-back gesture: once in a submenu the user
 * either releases on a leaf to select it, or releases in the hub to cancel the whole gesture.
 */
@Composable
private fun DwellNavigation(
    wheelState: WheelMenuState,
    navigator: WheelMenuNavigator,
) {
    LaunchedEffect(wheelState, navigator) {
        snapshotFlow { wheelState.isOpen to wheelState.hoveredIndex }
            .collectLatest { (open, hovered) ->
                if (!open) return@collectLatest
                val node = hovered?.let { navigator.current.getOrNull(it) }
                if (node?.children != null) {
                    delay(DWELL_MS)
                    navigator.drillInto(hovered!!)
                }
            }
    }
}

/** Pause over a parent slice before it drills into its submenu. */
private const val DWELL_MS = 280L

// Static previews render the closed (empty) state — the wheel opens only through the native
// gesture projection. Open-wheel visuals are previewed in the design-system WheelMenu previews.
@Preview(showBackground = true, widthDp = 400, heightDp = 400)
@Composable
private fun RootWheelScreenPreview() {
    HawkeyeTheme {
        RootWheelScreen(
            state = RootWheelState(
                root = listOf(
                    WheelNodeUi(
                        id = null,
                        label = UiText.DynamicString("Change View"),
                        accentColor = HawkeyeDronePalette.colors[0],
                        children = listOf(
                            WheelNodeUi(
                                id = "view:cam:0",
                                label = UiText.DynamicString("Chase"),
                                accentColor = HawkeyeDronePalette.colors[0],
                            ),
                        ),
                    ),
                ),
                gesture = SwarmWheelSnapshot.Idle.copy(droneCount = 1),
            ),
            onAction = {},
        )
    }
}
