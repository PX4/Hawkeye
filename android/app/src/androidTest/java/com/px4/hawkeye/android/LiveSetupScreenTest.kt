package com.px4.hawkeye.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.feature.live.presentation.LiveSetupAction
import com.px4.hawkeye.feature.live.presentation.LiveSetupScreen
import com.px4.hawkeye.feature.live.presentation.LiveSetupState
import org.junit.Rule
import org.junit.Test

/**
 * Renders the pure LiveSetupScreen with faked state — no renderer, no SITL, no device telemetry.
 */
class LiveSetupScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun endpoint_isShown_whenIpResolved() {
        composeRule.setContent {
            HawkeyeTheme {
                LiveSetupScreen(
                    state = LiveSetupState(
                        deviceIp = "192.168.1.42",
                        listenPort = 19410,
                        endpoint = "udp://192.168.1.42:19410",
                    ),
                    onAction = {},
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithText("udp://192.168.1.42:19410").assertIsDisplayed()
        composeRule.onNodeWithText("Start live session").assertIsDisplayed()
    }

    @Test
    fun retryButton_isShown_whenIpUnavailable() {
        composeRule.setContent {
            HawkeyeTheme {
                LiveSetupScreen(
                    state = LiveSetupState(deviceIp = null, listenPort = 19410, endpoint = ""),
                    onAction = {},
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun startButton_dispatchesStartAction() {
        val actions = mutableListOf<LiveSetupAction>()
        composeRule.setContent {
            HawkeyeTheme {
                LiveSetupScreen(
                    state = LiveSetupState(
                        deviceIp = "10.0.0.5",
                        listenPort = 19410,
                        endpoint = "udp://10.0.0.5:19410",
                    ),
                    onAction = { actions += it },
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithText("Start live session").performClick()
        assert(actions.contains(LiveSetupAction.OnStartLiveClicked))
    }

    @Test
    fun retryButton_dispatchesRefreshAction() {
        val actions = mutableListOf<LiveSetupAction>()
        composeRule.setContent {
            HawkeyeTheme {
                LiveSetupScreen(
                    state = LiveSetupState(deviceIp = null, listenPort = 19410, endpoint = ""),
                    onAction = { actions += it },
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithText("Retry").performClick()
        assert(actions.contains(LiveSetupAction.OnRefreshIp))
    }
}
