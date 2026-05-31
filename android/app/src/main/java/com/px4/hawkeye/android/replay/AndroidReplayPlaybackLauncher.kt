package com.px4.hawkeye.android.replay

import android.content.Context
import com.px4.hawkeye.android.render.RendererLauncher
import com.px4.hawkeye.core.presentation.ReplayPlaybackLauncher

/**
 * App-side implementation of the Replay feature's [ReplayPlaybackLauncher] seam. By the
 * time this runs the repository has already copied the selected log into the renderer
 * inbox and bumped the sentinel, so this only needs to start the renderer (which loads
 * from the inbox). [entryId] is unused for now.
 */
class AndroidReplayPlaybackLauncher : ReplayPlaybackLauncher {
    override fun launch(context: Context, entryId: String) {
        RendererLauncher.launch(context)
    }
}
