package com.px4.hawkeye.core.presentation

import android.content.Context

/**
 * Seam for launching the renderer in live (MAVLink/SITL) mode. Mirrors [ReplayPlaybackLauncher];
 * the presentation features cannot depend on `:app`, so the app provides the implementation via
 * Koin. No staged log to carry, hence no entry id.
 */
fun interface LivePlaybackLauncher {
    fun launch(context: Context)
}
