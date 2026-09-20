package com.px4.hawkeye.feature.live.presentation

sealed interface LiveSetupAction {
    data object OnStartLiveClicked : LiveSetupAction
    data object OnRefreshIp : LiveSetupAction

    /** Outcome of the ACCESS_LOCAL_NETWORK prompt the start click triggered on Android 17+. */
    data class OnLocalNetworkPermissionResult(val granted: Boolean) : LiveSetupAction

    /** Escape hatch after a permanent refusal, which the system prompt will not re-ask. */
    data object OnOpenAppSettingsClicked : LiveSetupAction
}
