package com.px4.hawkeye.feature.live.presentation

import com.px4.hawkeye.core.domain.DEFAULT_LIVE_PORT

data class LiveSetupState(
    val deviceIp: String? = null,
    val listenPort: Int = DEFAULT_LIVE_PORT,
    // Pre-formatted "udp://<ip>:<port>" the user points PX4 at; empty when the IP is unknown.
    val endpoint: String = "",
)
