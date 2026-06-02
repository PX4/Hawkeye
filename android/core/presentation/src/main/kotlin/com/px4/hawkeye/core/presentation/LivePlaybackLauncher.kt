package com.px4.hawkeye.core.presentation

import android.content.Context

/**
 * Seam for launching the renderer in live (MAVLink/SITL) mode. Mirrors [ReplayPlaybackLauncher];
 * the presentation features cannot depend on `:app`, so the app provides the implementation via
 * Koin. Carries the user-chosen MAVLink listen port through to the native renderer.
 */
fun interface LivePlaybackLauncher {
    fun launch(context: Context, listenPort: Int)
}
