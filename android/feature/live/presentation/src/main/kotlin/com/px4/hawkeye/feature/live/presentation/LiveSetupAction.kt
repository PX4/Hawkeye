package com.px4.hawkeye.feature.live.presentation

sealed interface LiveSetupAction {
    data object OnStartLiveClicked : LiveSetupAction
    data object OnRefreshIp : LiveSetupAction

    /** Outcome of the ACCESS_LOCAL_NETWORK prompt, once the user has answered it. */
    data class OnLocalNetworkPermissionResult(val granted: Boolean) : LiveSetupAction

    /** Escape hatch after a permanent refusal, which the system prompt will not re-ask. */
    data object OnOpenAppSettingsClicked : LiveSetupAction

    /**
     * Screen came back to the foreground. Granting from the system settings page produces
     * no activity result, so without this the refusal notice would outlive the refusal.
     */
    data object OnResumed : LiveSetupAction
}
