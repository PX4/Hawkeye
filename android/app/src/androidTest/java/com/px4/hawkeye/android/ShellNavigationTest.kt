package com.px4.hawkeye.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
        composeRule.onNodeWithText(LIVE_START_BUTTON).assertIsDisplayed()
        rotate()
        composeRule.onNodeWithText(LIVE_START_BUTTON).assertIsDisplayed()
    }

    @Test
    fun about_isReachableFromSettings_andSurvivesRotation() {
        composeRule.onNodeWithText(NAV_SETTINGS).performClick()
        // The row is the last thing on Settings and sits below the fold in landscape, and
        // performClick does not scroll: without this the tap lands off-screen and no
        // navigation happens.
        composeRule.onNodeWithText(SETTINGS_ABOUT_ROW).performScrollTo().performClick()
        composeRule.onNodeWithText(ABOUT_OFFICIAL_HEADER).assertIsDisplayed()
        rotate()
        composeRule.onNodeWithText(ABOUT_OFFICIAL_HEADER).assertIsDisplayed()
    }

    /**
     * End-to-end check on the packaged notices: the text only reaches the screen if the
     * assets/NOTICE.md symlink materialized, AGP packaged it, and AndroidAboutInfoProvider
     * read it off the asset manager. A unit test with a fake provider cannot catch a break
     * anywhere in that chain.
     */
    @Test
    fun about_showsBundledLicenseNotices_whenExpanded() {
        composeRule.onNodeWithText(NAV_SETTINGS).performClick()
        composeRule.onNodeWithText(SETTINGS_ABOUT_ROW).performScrollTo().performClick()
        composeRule.onNodeWithText(LICENSES_SHOW).performScrollTo().performClick()
        composeRule.onNodeWithText(RAYLIB_COPYRIGHT, substring = true).assertExists()
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
        const val HOME_CONNECT_CARD = "Connect to a vehicle"
        const val REPLAY_LIBRARY_TITLE = "Replay library"
        const val NAV_SETTINGS = "Settings"
        const val SETTINGS_THEME_HEADER = "Theme"
        const val LIVE_START_BUTTON = "Start live session"

        // The Settings row label and the About section header, not the word "About" itself:
        // that string is on both screens and would match ambiguously.
        const val SETTINGS_ABOUT_ROW = "Version, official builds, and licenses"
        const val ABOUT_OFFICIAL_HEADER = "Official builds"
        const val LICENSES_SHOW = "Show"

        // From the packaged NOTICE.md, so matching it proves the asset was read at runtime.
        const val RAYLIB_COPYRIGHT = "Ramon Santamaria"
    }
}
