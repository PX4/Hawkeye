package com.px4.hawkeye.android

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.px4.hawkeye.android.render.SwarmWheelSnapshot
import com.px4.hawkeye.android.render.wheel.RootWheelAction
import com.px4.hawkeye.android.render.wheel.RootWheelScreen
import com.px4.hawkeye.android.render.wheel.RootWheelState
import com.px4.hawkeye.android.render.wheel.WheelNodeUi
import com.px4.hawkeye.core.designsystem.HawkeyeDronePalette
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.core.designsystem.wheel.WheelMenuTestTags
import com.px4.hawkeye.core.presentation.UiText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives [RootWheelScreen] with scripted native gesture snapshots — the same sequence the
 * polling ViewModel produces — and verifies the projection into the wheel widget: open,
 * hover, one-shot selection on release, and close-without-selection paths. Most tests use a
 * flat ring of leaf nodes (releases dispatch directly); the nested tests use a two-level root
 * to cover dwell-to-drill and the deliberate absence of a hub-back gesture.
 */
@RunWith(AndroidJUnit4::class)
class RootWheelScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val robot by lazy { RootWheelRobot(composeRule) }

    @Test
    fun wheelAppearsWhileTheNativeGestureIsOpenAndClosesOnIdle() {
        robot
            .setContent()
            .assertWheelGone()
            .open(CENTER)
            .assertWheelVisible()
            .idle()
            .assertWheelGone()
            .assertSelections()
    }

    @Test
    fun releaseOverASliceSelectsThatLeafExactlyOnce() {
        robot
            .setContent()
            .open(CENTER)
            .moveTo(RIGHT_SLICE)
            .release(RIGHT_SLICE)
            .assertWheelGone()
            // A later unrelated snapshot with the same seq must not replay the selection.
            .idle()
            .assertSelections("drone:0")
    }

    @Test
    fun tornSnapshotWithStaleOpenPhaseDoesNotReopenAfterRelease() {
        // The native publish is not a single atomic block: a poll can pair a stale OPEN
        // phase with a fresh release seq. The release must win and the wheel stay closed.
        robot
            .setContent()
            .open(CENTER)
            .moveTo(RIGHT_SLICE)
            .releaseKeepingOpenPhase(RIGHT_SLICE)
            .assertWheelGone()
            .assertSelections("drone:0")
    }

    @Test
    fun rejectedGestureClosesWithoutSelection() {
        robot
            .setContent()
            .open(CENTER)
            .moveTo(RIGHT_SLICE)
            .reject()
            .assertWheelGone()
            .assertSelections()
    }

    @Test
    fun releaseInTheHubDeadZoneSelectsNothing() {
        robot
            .setContent()
            .open(CENTER)
            .moveTo(RIGHT_SLICE)
            .release(CENTER)
            .assertWheelGone()
            .assertSelections()
    }

    @Test
    fun dwellOnAParentDrillsInThenReleaseSelectsAChildLeaf() {
        // Hold, dwell on the "Views" branch so it drills into its submenu, then release on the
        // first child slice (Chase). A successful drill is proven by the child id dispatching —
        // releasing on the parent itself would dispatch nothing.
        robot
            .setNestedContent()
            .open(CENTER).settle()
            .moveTo(RIGHT_SLICE).settle()  // dwell on "Views" (root slice 0) drills into it
            .release(RIGHT_SLICE).settle() // submenu slice 0 = Chase
            .assertWheelGone()
            .assertSelections("view:cam:0")
    }

    @Test
    fun dwellingInTheHubDoesNotStepBackALevel() {
        // Regression for the removed hub-back gesture: after drilling in, dwelling in the hub
        // must NOT pop to the root. If it did, the final slice would be the "Views" parent (no
        // dispatch); selecting Chase proves we stayed in the submenu.
        robot
            .setNestedContent()
            .open(CENTER).settle()
            .moveTo(RIGHT_SLICE).settle()  // dwell on "Views" drills in
            .moveTo(CENTER).settle()       // finger into the hub; old hub-back would pop here
            .moveTo(RIGHT_SLICE).settle()  // still the submenu: slice 0 = Chase
            .release(RIGHT_SLICE).settle()
            .assertWheelGone()
            .assertSelections("view:cam:0")
    }

    private companion object {
        // Well inside any screen; slice offset far beyond the 64 dp hub dead zone.
        val CENTER = Offset(540f, 800f)
        val RIGHT_SLICE = Offset(540f + 400f, 800f) // 3 o'clock = slice 0 of two
    }
}

private class RootWheelRobot(private val rule: ComposeContentTestRule) {

    private lateinit var state: MutableState<RootWheelState>
    private val actions = mutableListOf<RootWheelAction>()

    fun setContent() = apply {
        state = mutableStateOf(
            RootWheelState(
                root = listOf(
                    WheelNodeUi(
                        id = "drone:0",
                        label = UiText.DynamicString("Drone 1"),
                        hubLabel = UiText.DynamicString("alpha.ulg"),
                        accentColor = HawkeyeDronePalette.colors[0],
                    ),
                    WheelNodeUi(
                        id = "drone:1",
                        label = UiText.DynamicString("Drone 2"),
                        hubLabel = UiText.DynamicString("bravo.ulg"),
                        accentColor = HawkeyeDronePalette.colors[1],
                    ),
                ),
                gesture = SwarmWheelSnapshot.Idle.copy(droneCount = 2),
            ),
        )
        rule.setContent {
            HawkeyeTheme {
                RootWheelScreen(state = state.value, onAction = { actions += it })
            }
        }
    }

    /**
     * A two-level root (parents with leaf children), like the real swarm session, for the
     * nested drill tests.
     */
    fun setNestedContent() = apply {
        // Pause the clock so [settle] advances it deterministically (the dwell uses delay()).
        rule.mainClock.autoAdvance = false
        state = mutableStateOf(
            RootWheelState(
                root = listOf(
                    WheelNodeUi(
                        id = null, // parent: drilled into on dwell, never dispatched
                        label = UiText.DynamicString("Views"),
                        hubLabel = UiText.DynamicString("Change View"),
                        accentColor = HawkeyeDronePalette.colors[0],
                        children = listOf(
                            WheelNodeUi("view:cam:0", UiText.DynamicString("Chase"), HawkeyeDronePalette.colors[0]),
                            WheelNodeUi("view:cam:1", UiText.DynamicString("FPV"), HawkeyeDronePalette.colors[0]),
                        ),
                    ),
                    WheelNodeUi(
                        id = null,
                        label = UiText.DynamicString("Drones"),
                        hubLabel = UiText.DynamicString("Select Drone"),
                        accentColor = HawkeyeDronePalette.colors[1],
                        children = listOf(
                            WheelNodeUi("drone:0", UiText.DynamicString("Drone 1"), HawkeyeDronePalette.colors[0]),
                            WheelNodeUi("drone:1", UiText.DynamicString("Drone 2"), HawkeyeDronePalette.colors[1]),
                        ),
                    ),
                ),
                gesture = SwarmWheelSnapshot.Idle.copy(droneCount = 2),
            ),
        )
        rule.setContent {
            HawkeyeTheme {
                RootWheelScreen(state = state.value, onAction = { actions += it })
            }
        }
    }

    /**
     * Advance the paused test clock so pending gesture effects dispatch and any dwell timer
     * fires, then sync. 500 ms comfortably exceeds RootWheelScreen's DWELL_MS (280 ms). Call
     * after each scripted gesture in a nested-content test.
     */
    fun settle() = apply {
        rule.mainClock.advanceTimeBy(500L)
        rule.waitForIdle()
    }

    private fun gesture(transform: (SwarmWheelSnapshot) -> SwarmWheelSnapshot) = apply {
        state.value = state.value.copy(gesture = transform(state.value.gesture))
        rule.waitForIdle()
    }

    fun open(at: Offset) = gesture {
        it.copy(
            phase = SwarmWheelSnapshot.PHASE_OPEN,
            centerX = at.x, centerY = at.y,
            fingerX = at.x, fingerY = at.y,
        )
    }

    fun moveTo(finger: Offset) = gesture { it.copy(fingerX = finger.x, fingerY = finger.y) }

    fun release(at: Offset) = gesture {
        it.copy(
            phase = SwarmWheelSnapshot.PHASE_IDLE,
            releaseSeq = it.releaseSeq + 1,
            releaseX = at.x, releaseY = at.y,
        )
    }

    /** A torn read: the seq bumped but the phase field still carries the stale OPEN. */
    fun releaseKeepingOpenPhase(at: Offset) = gesture {
        it.copy(
            phase = SwarmWheelSnapshot.PHASE_OPEN,
            releaseSeq = it.releaseSeq + 1,
            releaseX = at.x, releaseY = at.y,
        )
    }

    fun reject() = gesture { it.copy(phase = SwarmWheelSnapshot.PHASE_REJECTED) }

    fun idle() = gesture { it.copy(phase = SwarmWheelSnapshot.PHASE_IDLE) }

    fun assertWheelVisible() = apply {
        rule.onNodeWithTag(WheelMenuTestTags.WHEEL).assertIsDisplayed()
    }

    fun assertWheelGone() = apply {
        rule.onNodeWithTag(WheelMenuTestTags.WHEEL).assertDoesNotExist()
    }

    fun assertSelections(vararg ids: String) = apply {
        assertEquals(ids.map { RootWheelAction.OnNodeSelected(it) }, actions)
    }
}
