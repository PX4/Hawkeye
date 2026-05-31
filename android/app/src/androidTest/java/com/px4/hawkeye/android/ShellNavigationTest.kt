package com.px4.hawkeye.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the Compose shell on a device: navigates to every destination and rotates each
 * one. Rotation is exercised with [androidx.test.core.app.ActivityScenario.recreate],
 * which runs the same activity destroy/recreate + state-restoration path a real
 * orientation change triggers. Each screen must survive it (no crash, content intact),
 * and the selected destination must be preserved (it lives in ShellViewModel).
 *
 * The native renderer (HawkeyeActivity) is intentionally out of scope here: it runs in a
 * separate `:renderer` process, is landscape-locked, and hard-exits on teardown.
 */
@RunWith(AndroidJUnit4::class)
class ShellNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun rotate() {
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
    }

    @Test
    fun home_isShown_andSurvivesRotation() {
        composeRule.onNodeWithText(HOME_REPLAY_CARD).assertIsDisplayed()
        rotate()
        composeRule.onNodeWithText(HOME_REPLAY_CARD).assertIsDisplayed()
    }

    @Test
    fun replayLibrary_isReachableFromHome_andSurvivesRotation() {
        composeRule.onNodeWithText(HOME_REPLAY_CARD).performClick()
        composeRule.onNodeWithText(REPLAY_LIBRARY_TITLE).assertIsDisplayed()
        rotate()
        composeRule.onNodeWithText(REPLAY_LIBRARY_TITLE).assertIsDisplayed()
    }

    @Test
    fun settings_isReachableFromNavBar_andSurvivesRotation() {
        composeRule.onNodeWithText(NAV_SETTINGS).performClick()
        composeRule.onNodeWithText(SETTINGS_THEME_HEADER).assertIsDisplayed()
        rotate()
        composeRule.onNodeWithText(SETTINGS_THEME_HEADER).assertIsDisplayed()
    }

    @Test
    fun live_isReachableFromHome_andSurvivesRotation() {
        composeRule.onNodeWithText(HOME_CONNECT_CARD).performClick()
        composeRule.onNodeWithText(LIVE_COMING_SOON).assertIsDisplayed()
        rotate()
        composeRule.onNodeWithText(LIVE_COMING_SOON).assertIsDisplayed()
    }

    @Test
    fun selectedDestination_isPreservedAcrossRotation() {
        composeRule.onNodeWithText(NAV_SETTINGS).performClick()
        composeRule.onNodeWithText(SETTINGS_THEME_HEADER).assertIsDisplayed()

        rotate()

        // Still on Settings — not reset to Home — proving the back stack in ShellViewModel
        // survived the config change.
        composeRule.onNodeWithText(SETTINGS_THEME_HEADER).assertIsDisplayed()
    }

    private companion object {
        const val HOME_REPLAY_CARD = "Replay a flight"
        const val HOME_CONNECT_CARD = "Connect to a simulator"
        const val REPLAY_LIBRARY_TITLE = "Replay library"
        const val NAV_SETTINGS = "Settings"
        const val SETTINGS_THEME_HEADER = "Theme"
        const val LIVE_COMING_SOON = "Connecting to a simulator is coming in Plan 3."
    }
}
