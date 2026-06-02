package com.px4.hawkeye.android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.feature.home.presentation.HomeScreen
import com.px4.hawkeye.feature.home.presentation.HomeState
import com.px4.hawkeye.feature.home.presentation.RecentFlightUi
import org.junit.Rule
import org.junit.Test

/**
 * Renders [HomeScreen] directly with an injected [WindowSizeClass] so the layout branch is
 * deterministic, inside a forced-size [Box] that controls the physical viewport. Verifies
 * the landscape regression fix: items stay reachable (compact, short viewport scrolls) and
 * the two-pane split renders header + cards together (medium width).
 */
class HomeScreenLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val state = HomeState(
        recents = listOf(
            RecentFlightUi("1", RECENT_NAME, "May 30, 2026"),
        ),
    )

    private fun setHome(
        widthDp: Int,
        heightDp: Int,
        sizeClass: WindowSizeClass,
        homeState: HomeState = state,
    ) {
        composeRule.setContent {
            HawkeyeTheme(darkTheme = true) {
                Box(modifier = Modifier.size(width = widthDp.dp, height = heightDp.dp)) {
                    HomeScreen(state = homeState, onAction = {}, windowSizeClass = sizeClass)
                }
            }
        }
    }

    @Test
    fun compactShortViewport_recentsReachableByScrolling() {
        // Compact width + a deliberately short viewport: the recents row starts off-screen.
        setHome(widthDp = 360, heightDp = 320, sizeClass = WindowSizeClass(360, 320))

        // performScrollTo only succeeds inside a scrollable container, proving the scroll
        // fallback makes the previously-clipped recents reachable.
        composeRule.onNodeWithText(RECENT_NAME).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun mediumWidthViewport_showsHeaderAndBothCardsTogether() {
        // The left column now stacks the header above both cards, so it is taller than the
        // old cards-only pane; a 500dp-tall viewport fits all three without scrolling.
        setHome(widthDp = 900, heightDp = 500, sizeClass = WindowSizeClass(900, 500))

        // Two-pane split: title + both action cards together in the left column.
        composeRule.onNodeWithText(HOME_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(REPLAY_CARD).assertIsDisplayed()
        composeRule.onNodeWithText(CONNECT_CARD).assertIsDisplayed()
    }

    @Test
    fun mediumWidthViewport_showsRecentsInRightColumn() {
        // The recents list lives in its own right column in the wide layout; with room to
        // spare it renders without needing to scroll.
        setHome(widthDp = 900, heightDp = 500, sizeClass = WindowSizeClass(900, 500))

        composeRule.onNodeWithText(RECENT_HEADER).assertIsDisplayed()
        composeRule.onNodeWithText(RECENT_NAME).assertIsDisplayed()
    }

    @Test
    fun compactViewport_bothModeCardsPresent() {
        setHome(widthDp = 360, heightDp = 800, sizeClass = WindowSizeClass(360, 800))

        composeRule.onNodeWithText(REPLAY_CARD).assertIsDisplayed()
        composeRule.onNodeWithText(CONNECT_CARD).assertIsDisplayed()
    }

    @Test
    fun atMediumBreakpoint_usesTwoPaneLayout() {
        // Exactly the M3 medium-width boundary (600dp): the two-pane split must engage,
        // showing the hero title and both cards together end-to-end (not just the unit-tested
        // predicate arithmetic).
        setHome(widthDp = 600, heightDp = 500, sizeClass = WindowSizeClass(600, 500))

        composeRule.onNodeWithText(HOME_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(REPLAY_CARD).assertIsDisplayed()
        composeRule.onNodeWithText(CONNECT_CARD).assertIsDisplayed()
    }

    @Test
    fun emptyRecents_hidesRecentsSection() {
        // The recents section is guarded by state.recents.isNotEmpty() in both layouts; with
        // no recents the header must be absent while the mode cards still render.
        setHome(
            widthDp = 360,
            heightDp = 800,
            sizeClass = WindowSizeClass(360, 800),
            homeState = HomeState(recents = emptyList()),
        )

        composeRule.onNodeWithText(RECENT_HEADER).assertDoesNotExist()
        composeRule.onNodeWithText(REPLAY_CARD).assertIsDisplayed()
        composeRule.onNodeWithText(CONNECT_CARD).assertIsDisplayed()
    }

    private companion object {
        const val HOME_TITLE = "Hawkeye"
        const val REPLAY_CARD = "Replay a flight"
        const val CONNECT_CARD = "Connect to a vehicle"
        const val RECENT_HEADER = "Recent flights"
        const val RECENT_NAME = "flight_log.ulg"
    }
}
