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
import com.px4.hawkeye.android.render.swarm.DroneWheelItemUi
import com.px4.hawkeye.android.render.swarm.SwarmWheelAction
import com.px4.hawkeye.android.render.swarm.SwarmWheelScreen
import com.px4.hawkeye.android.render.swarm.SwarmWheelState
import com.px4.hawkeye.core.designsystem.HawkeyeDronePalette
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.core.designsystem.wheel.WheelMenuTestTags
import com.px4.hawkeye.core.presentation.UiText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives [SwarmWheelScreen] with scripted native gesture snapshots — the same sequence the
 * polling ViewModel produces — and verifies the projection into the wheel widget: open,
 * hover, one-shot selection on release, and close-without-selection paths.
 */
@RunWith(AndroidJUnit4::class)
class SwarmWheelScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val robot by lazy { SwarmWheelRobot(composeRule) }

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
    fun releaseOverASliceSelectsThatDroneExactlyOnce() {
        robot
            .setContent()
            .open(CENTER)
            .moveTo(RIGHT_SLICE)
            .release(RIGHT_SLICE)
            .assertWheelGone()
            // A later unrelated snapshot with the same seq must not replay the selection.
            .idle()
            .assertSelections(0)
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

    private companion object {
        // Well inside any screen; slice offset far beyond the 64 dp hub dead zone.
        val CENTER = Offset(540f, 800f)
        val RIGHT_SLICE = Offset(540f + 400f, 800f) // 3 o'clock = slice 0 of two
    }
}

private class SwarmWheelRobot(private val rule: ComposeContentTestRule) {

    private lateinit var state: MutableState<SwarmWheelState>
    private val actions = mutableListOf<SwarmWheelAction>()

    fun setContent() = apply {
        state = mutableStateOf(
            SwarmWheelState(
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
        )
        rule.setContent {
            HawkeyeTheme {
                SwarmWheelScreen(state = state.value, onAction = { actions += it })
            }
        }
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

    fun reject() = gesture { it.copy(phase = SwarmWheelSnapshot.PHASE_REJECTED) }

    fun idle() = gesture { it.copy(phase = SwarmWheelSnapshot.PHASE_IDLE) }

    fun assertWheelVisible() = apply {
        rule.onNodeWithTag(WheelMenuTestTags.WHEEL).assertIsDisplayed()
    }

    fun assertWheelGone() = apply {
        rule.onNodeWithTag(WheelMenuTestTags.WHEEL).assertDoesNotExist()
    }

    fun assertSelections(vararg drones: Int) = apply {
        assertEquals(drones.map { SwarmWheelAction.OnDroneSelected(it) }, actions)
    }
}
