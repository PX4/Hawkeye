package com.px4.hawkeye.feature.live.presentation

sealed interface LiveSetupEvent {
    /** Local network access is in hand; hand off to the renderer in live mode. */
    data object LaunchLiveSession : LiveSetupEvent

    /** Show the system ACCESS_LOCAL_NETWORK prompt. Only the host can raise it. */
    data object RequestLocalNetworkPermission : LiveSetupEvent

    /** Send the user to this app's system settings page to grant Nearby devices by hand. */
    data object OpenAppSettings : LiveSetupEvent
}
