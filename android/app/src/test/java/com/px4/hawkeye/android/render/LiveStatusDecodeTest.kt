package com.px4.hawkeye.android.render

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.px4.hawkeye.feature.live.domain.LiveConnectionState
import org.junit.jupiter.api.Test

class LiveStatusDecodeTest {

    @Test fun `empty array falls back to waiting`() {
        assertThat(decodeLiveStatus(floatArrayOf()))
            .isEqualTo(LiveStatus(LiveConnectionState.WAITING, sysid = 0, port = 0))
    }

    @Test fun `short array falls back to waiting`() {
        assertThat(decodeLiveStatus(floatArrayOf(1f, 2f)))
            .isEqualTo(LiveStatus(LiveConnectionState.WAITING, sysid = 0, port = 0))
    }

    @Test fun `state 0 decodes to waiting with port`() {
        assertThat(decodeLiveStatus(floatArrayOf(0f, 0f, 19410f)))
            .isEqualTo(LiveStatus(LiveConnectionState.WAITING, sysid = 0, port = 19410))
    }

    @Test fun `state 1 decodes to connected with sysid and port`() {
        assertThat(decodeLiveStatus(floatArrayOf(1f, 7f, 14550f)))
            .isEqualTo(LiveStatus(LiveConnectionState.CONNECTED, sysid = 7, port = 14550))
    }

    @Test fun `state 2 decodes to lost`() {
        assertThat(decodeLiveStatus(floatArrayOf(2f, 0f, 19410f)))
            .isEqualTo(LiveStatus(LiveConnectionState.LOST, sysid = 0, port = 19410))
    }

    @Test fun `unknown state code falls back to waiting`() {
        assertThat(decodeLiveStatus(floatArrayOf(99f, 3f, 19410f)))
            .isEqualTo(LiveStatus(LiveConnectionState.WAITING, sysid = 3, port = 19410))
    }

    @Test fun `max sysid and port survive the float round-trip`() {
        assertThat(decodeLiveStatus(floatArrayOf(1f, 255f, 65535f)))
            .isEqualTo(LiveStatus(LiveConnectionState.CONNECTED, sysid = 255, port = 65535))
    }
}
