package com.px4.hawkeye.feature.home.presentation

import com.px4.hawkeye.core.presentation.UiText

sealed interface HomeEvent {
    data object NavigateToReplay : HomeEvent
    /** Connect to a simulator: launch the renderer in live mode. */
    data object ConnectLive : HomeEvent

    /** A recent log was staged into the inbox; hand off to the renderer. */
    data class PlayRecent(val entryId: String) : HomeEvent

    data class ShowError(val text: UiText) : HomeEvent
}
