package com.px4.hawkeye.feature.home.presentation

sealed interface HomeEvent {
    data object NavigateToReplay : HomeEvent
    data object NavigateToLive : HomeEvent
}
