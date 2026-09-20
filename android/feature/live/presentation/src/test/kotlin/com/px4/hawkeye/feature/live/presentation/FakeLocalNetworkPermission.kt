package com.px4.hawkeye.feature.live.presentation

import com.px4.hawkeye.core.domain.LocalNetworkPermission

/** Flippable so a test can model a grant arriving between two calls. */
class FakeLocalNetworkPermission(var granted: Boolean) : LocalNetworkPermission {
    override fun isGranted(): Boolean = granted
}
