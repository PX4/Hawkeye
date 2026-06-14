package com.px4.hawkeye.feature.home.presentation

import com.px4.hawkeye.core.presentation.UiText

sealed interface HomeEvent {
    data object NavigateToReplay : HomeEvent
    /** Connect to a vehicle (simulated or real): launch the renderer in live mode. */
    data object ConnectLive : HomeEvent

    /**
     * A recent log was staged into the inbox; hand off to the renderer. [displayName] is
     * forwarded as the session's single drone label.
     */
    data class PlayRecent(val displayName: String) : HomeEvent

    data class ShowError(val text: UiText) : HomeEvent
}
