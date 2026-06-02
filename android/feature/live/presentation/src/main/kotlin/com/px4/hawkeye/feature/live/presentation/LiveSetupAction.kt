package com.px4.hawkeye.feature.live.presentation

sealed interface LiveSetupAction {
    data object OnStartLiveClicked : LiveSetupAction
    data object OnRefreshIp : LiveSetupAction
}
