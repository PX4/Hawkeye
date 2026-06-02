package com.px4.hawkeye.feature.live.presentation

sealed interface LiveSetupEvent {
    /** User confirmed; hand off to the renderer in live mode. */
    data object LaunchLiveSession : LiveSetupEvent
}
