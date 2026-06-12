package com.px4.hawkeye.android.replay

import android.content.Context
import com.px4.hawkeye.android.render.RendererLauncher
import com.px4.hawkeye.core.presentation.ReplayPlaybackLauncher

/**
 * App-side implementation of the Replay feature's [ReplayPlaybackLauncher] seam. By the
 * time this runs the repository has already copied the selected logs into the renderer
 * inbox and bumped the sentinel, so this only starts the renderer (which loads from the
 * inbox) and forwards the drone labels for the swarm wheel overlay.
 */
class AndroidReplayPlaybackLauncher : ReplayPlaybackLauncher {
    override fun launch(context: Context, droneLabels: List<String>) {
        RendererLauncher.launch(context, droneLabels = droneLabels)
    }
}
