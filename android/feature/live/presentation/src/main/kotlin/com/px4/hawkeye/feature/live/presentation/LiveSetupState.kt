package com.px4.hawkeye.feature.live.presentation

import com.px4.hawkeye.core.domain.DEFAULT_LIVE_PORT

data class LiveSetupState(
    val deviceIp: String? = null,
    val listenPort: Int = DEFAULT_LIVE_PORT,
    // Pre-formatted "udp://<ip>:<port>" the user points PX4 at; empty when the IP is unknown.
    val endpoint: String = "",
    // Set when the user turned down the local-network prompt. A live session cannot receive
    // telemetry without it, so the screen explains that instead of launching a renderer that
    // would sit at "waiting" forever.
    val localNetworkDenied: Boolean = false,
)
