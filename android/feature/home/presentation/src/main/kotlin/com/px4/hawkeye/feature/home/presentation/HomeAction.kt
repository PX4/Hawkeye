package com.px4.hawkeye.feature.home.presentation

sealed interface HomeAction {
    data object OnReplayClicked : HomeAction
    data object OnConnectClicked : HomeAction
}
