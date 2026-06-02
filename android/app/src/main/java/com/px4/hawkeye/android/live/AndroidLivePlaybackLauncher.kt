package com.px4.hawkeye.android.live

import android.content.Context
import com.px4.hawkeye.android.render.RenderMode
import com.px4.hawkeye.android.render.RendererLauncher
import com.px4.hawkeye.core.presentation.LivePlaybackLauncher

/** App-side implementation of [LivePlaybackLauncher]: starts the renderer in live mode. */
class AndroidLivePlaybackLauncher : LivePlaybackLauncher {
    override fun launch(context: Context) {
        RendererLauncher.launch(context, RenderMode.LIVE)
    }
}
