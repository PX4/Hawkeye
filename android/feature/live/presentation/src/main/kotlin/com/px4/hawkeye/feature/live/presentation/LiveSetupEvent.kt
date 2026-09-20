package com.px4.hawkeye.feature.live.presentation

sealed interface LiveSetupEvent {
    /** User confirmed; hand off to the renderer in live mode once the permission allows it. */
    data object LaunchLiveSession : LiveSetupEvent

    /** Send the user to this app's system settings page to grant Nearby devices by hand. */
    data object OpenAppSettings : LiveSetupEvent
}
