package com.px4.hawkeye.feature.about.presentation

sealed interface AboutAction {
    /** Expand or collapse the bundled license notices, which are long enough to bury the rest. */
    data object OnToggleLicenses : AboutAction
}
