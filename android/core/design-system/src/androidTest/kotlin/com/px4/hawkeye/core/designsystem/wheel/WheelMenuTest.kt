package com.px4.hawkeye.core.designsystem.wheel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class WheelMenuTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun items() = listOf(
        WheelMenuItem("One", Color.White),
        WheelMenuItem("Two", Color.Cyan),
        WheelMenuItem("Three", Color.Green),
        WheelMenuItem("Four", Color.Yellow),
        WheelMenuItem("Five", Color.Red),
        WheelMenuItem("Six", Color.Magenta),
    )

    /** Clock advance that comfortably crosses the device's long-press timeout. */
    private var longPressWaitMs = 0L

    /** Hosts the widget plus the reference producer; returns the shared state. */
    private fun setWheelContent(onSelected: (Int) -> Unit): WheelMenuState {
        lateinit var state: WheelMenuState
        composeRule.setContent {
            HawkeyeTheme(darkTheme = true) {
                state = rememberWheelMenuState(items())
                longPressWaitMs =
                    LocalViewConfiguration.current.longPressTimeoutMillis + LONG_PRESS_MARGIN_MS
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(HOST)
                        .wheelMenuGestures(state, onSelected),
                ) {
                    WheelMenu(state = state, selectHint = "Release to select")
                }
            }
        }
        return state
    }

    @Test
    fun wheelIsHidden_untilOpened_andShowsWhenDriven() {
        val state = setWheelContent(onSelected = {})
        composeRule.onNodeWithTag(WheelMenuTestTags.WHEEL).assertDoesNotExist()

        // Drive the state directly: the seam any producer (including a native bridge) uses.
        composeRule.runOnIdle { state.open(Offset(540f, 540f)) }

        composeRule.onNodeWithTag(WheelMenuTestTags.WHEEL).assertIsDisplayed()
    }

    @Test
    fun longPress_opensTheWheel() {
        setWheelContent(onSelected = {})

        composeRule.onNodeWithTag(HOST).performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(longPressWaitMs)

        composeRule.onNodeWithTag(WheelMenuTestTags.WHEEL).assertIsDisplayed()

        composeRule.onNodeWithTag(HOST).performTouchInput { up() }
    }

    @Test
    fun quickTapOrDrag_neverOpens() {
        setWheelContent(onSelected = {})

        // Quick tap: released long before the long-press timeout.
        composeRule.onNodeWithTag(HOST).performTouchInput {
            down(center)
            up()
        }
        composeRule.onNodeWithTag(WheelMenuTestTags.WHEEL).assertDoesNotExist()

        // Drag past touch slop, then hold past the timeout: a pan/orbit, not a wheel
        // press. awaitLongPressOrCancellation alone would still fire here (it never
        // cancels on movement); the producer's own slop check keeps the wheel closed.
        composeRule.onNodeWithTag(HOST).performTouchInput {
            down(center)
            moveBy(Offset(300f, 0f))
        }
        composeRule.mainClock.advanceTimeBy(longPressWaitMs)
        composeRule.onNodeWithTag(WheelMenuTestTags.WHEEL).assertDoesNotExist()

        composeRule.onNodeWithTag(HOST).performTouchInput { up() }
        composeRule.onNodeWithTag(WheelMenuTestTags.WHEEL).assertDoesNotExist()
    }

    @Test
    fun fullGestureLoop_longPressDragRelease_selectsTheSlice() {
        var selected: Int? = null
        val state = setWheelContent(onSelected = { selected = it })

        composeRule.onNodeWithTag(HOST).performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(longPressWaitMs)
        composeRule.onNodeWithTag(WheelMenuTestTags.WHEEL).assertIsDisplayed()

        // Drag to 3 o'clock: slice 1 of 6 (well past the 64dp hub dead zone).
        composeRule.onNodeWithTag(HOST).performTouchInput { moveBy(Offset(500f, 0f)) }
        composeRule.runOnIdle { assertEquals(1, state.hoveredIndex) }
        composeRule.onNodeWithTag(HOST).performTouchInput { up() }

        composeRule.runOnIdle { assertEquals(1, selected) }
        composeRule.onNodeWithTag(WheelMenuTestTags.WHEEL).assertDoesNotExist()
    }

    @Test
    fun releaseInTheHub_cancelsWithoutSelection() {
        var selected: Int? = null
        setWheelContent(onSelected = { selected = it })

        composeRule.onNodeWithTag(HOST).performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(longPressWaitMs)
        composeRule.onNodeWithTag(WheelMenuTestTags.WHEEL).assertIsDisplayed()

        composeRule.onNodeWithTag(HOST).performTouchInput { up() } // still at the center: dead zone

        composeRule.runOnIdle { assertNull(selected) }
        composeRule.onNodeWithTag(WheelMenuTestTags.WHEEL).assertDoesNotExist()
    }

    @Test
    fun secondFinger_cancelsWithoutSelection() {
        var selected: Int? = null
        setWheelContent(onSelected = { selected = it })

        composeRule.onNodeWithTag(HOST).performTouchInput { down(0, center) }
        composeRule.mainClock.advanceTimeBy(longPressWaitMs)
        composeRule.onNodeWithTag(WheelMenuTestTags.WHEEL).assertIsDisplayed()

        composeRule.onNodeWithTag(HOST).performTouchInput {
            down(1, center + Offset(600f, 0f))
        }
        composeRule.onNodeWithTag(WheelMenuTestTags.WHEEL).assertDoesNotExist()

        composeRule.onNodeWithTag(HOST).performTouchInput {
            up(0)
            up(1)
        }
        composeRule.runOnIdle { assertNull(selected) }
    }

    private companion object {
        const val HOST = "wheel_host"

        // Added to the device's reported long-press timeout to absorb clock granularity.
        const val LONG_PRESS_MARGIN_MS = 250L
    }
}
