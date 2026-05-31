package com.px4.hawkeye.core.presentation

import android.content.Context

/**
 * Seam for launching the native renderer. The presentation features (Replay, Home) cannot
 * depend on `:app` — where the renderer Activity and its launcher live — so the app
 * provides an implementation through Koin and the feature screens invoke it once a log has
 * been staged for playback. Shared here in `core:presentation` because both the Replay
 * library and the Home recents peek launch playback.
 */
fun interface ReplayPlaybackLauncher {
    fun launch(context: Context, entryId: String)
}
