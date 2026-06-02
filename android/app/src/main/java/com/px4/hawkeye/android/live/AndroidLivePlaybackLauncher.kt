package com.px4.hawkeye.android.live

import android.content.Context
import com.px4.hawkeye.android.render.RenderMode
import com.px4.hawkeye.android.render.RendererLauncher
import com.px4.hawkeye.core.domain.DeviceIpProvider
import com.px4.hawkeye.core.presentation.LivePlaybackLauncher

/**
 * App-side implementation of [LivePlaybackLauncher]: starts the renderer in live mode with the
 * user-chosen listen port and the device's LAN IP (resolved here; the :renderer process has no
 * Koin to inject a provider, so the IP rides along as an Intent extra).
 */
class AndroidLivePlaybackLauncher(
    private val deviceIpProvider: DeviceIpProvider,
) : LivePlaybackLauncher {
    override fun launch(context: Context, listenPort: Int) {
        RendererLauncher.launch(
            context,
            mode = RenderMode.LIVE,
            listenPort = listenPort,
            deviceIp = deviceIpProvider.localIpAddress(),
        )
    }
}
