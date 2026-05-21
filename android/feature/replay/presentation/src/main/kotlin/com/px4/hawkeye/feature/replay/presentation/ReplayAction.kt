package com.px4.hawkeye.feature.replay.presentation

sealed interface ReplayAction {
    /**
     * Fired by `HawkeyeActivity` on cold launch. [fromFreshIngest] is true only when
     * the activity was started by `IntentRouterActivity` right after it ingested a
     * new `.ulg` into the inbox — in that case the inbox holds a file the user
     * explicitly asked for, so we leave it alone and the native poll loop picks it up.
     *
     * Otherwise (user tapped the app icon, no fresh share), the inbox may still hold
     * a stale `current.ulg` from a previous session. We wipe it so the renderer
     * starts on the empty-state HUD and surface the "No file loaded" dialog.
     */
    data class OnAppStarted(val fromFreshIngest: Boolean) : ReplayAction

    /**
     * Fired by `IntentRouterActivity` when an inbound VIEW/SEND intent arrives.
     * Triggers preview resolution and shows the "Open ULog?" confirm dialog.
     */
    data class OnIntentReceived(val uri: String) : ReplayAction

    data object OnConfirmOpen : ReplayAction
    data object OnDismissDialog : ReplayAction
}
