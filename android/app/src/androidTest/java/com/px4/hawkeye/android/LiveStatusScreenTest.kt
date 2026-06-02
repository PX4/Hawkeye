package com.px4.hawkeye.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.px4.hawkeye.android.render.LiveStatus
import com.px4.hawkeye.android.render.live.LiveStatusScreen
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.feature.live.domain.LiveConnectionState
import org.junit.Rule
import org.junit.Test

/** Renders the pure live-status strip for each connection state — no native engine. */
class LiveStatusScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(status: LiveStatus, deviceIp: String?) {
        composeRule.setContent { HawkeyeTheme { LiveStatusScreen(status = status, deviceIp = deviceIp) } }
    }

    @Test
    fun waiting_showsEndpoint() {
        render(LiveStatus(LiveConnectionState.WAITING, sysid = 0, port = 19410), deviceIp = "192.168.1.42")
        composeRule.onNodeWithText("192.168.1.42:19410", substring = true).assertIsDisplayed()
    }

    @Test
    fun waiting_withoutIp_showsPortFallback() {
        render(LiveStatus(LiveConnectionState.WAITING, sysid = 0, port = 19410), deviceIp = null)
        composeRule.onNodeWithText("port 19410", substring = true).assertIsDisplayed()
    }

    @Test
    fun connected_showsSysid() {
        render(LiveStatus(LiveConnectionState.CONNECTED, sysid = 7, port = 19410), deviceIp = "192.168.1.42")
        composeRule.onNodeWithText("sysid 7", substring = true).assertIsDisplayed()
    }

    @Test
    fun lost_showsLostMessage() {
        render(LiveStatus(LiveConnectionState.LOST, sysid = 0, port = 19410), deviceIp = "192.168.1.42")
        composeRule.onNodeWithText("Connection lost", substring = true).assertIsDisplayed()
    }
}
