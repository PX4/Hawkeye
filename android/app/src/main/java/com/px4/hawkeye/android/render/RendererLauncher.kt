package com.px4.hawkeye.android.render

import android.content.Context
import android.content.Intent
import com.px4.hawkeye.android.HawkeyeActivity
import com.px4.hawkeye.core.domain.DEFAULT_LIVE_PORT
import java.io.File

/**
 * Single place the shell uses to launch the native renderer. Replay logs are delivered out
 * of band (the repository stages them into `filesDir/inbox/`). For [RenderMode.LIVE] we drop
 * a `inbox/.live` marker ("<millis> <port>") that the native startup compares against the
 * replay `.ready` token (newest wins) and uses to bind the listen port, and pass the mode
 * (and the device IP, for the live status overlay) as Intent extras.
 */
object RendererLauncher {

    const val EXTRA_MODE = "com.px4.hawkeye.android.render.MODE"
    const val EXTRA_DEVICE_IP = "com.px4.hawkeye.android.render.DEVICE_IP"

    fun launch(
        context: Context,
        mode: RenderMode = RenderMode.REPLAY,
        listenPort: Int = DEFAULT_LIVE_PORT,
        deviceIp: String? = null,
    ) {
        if (mode == RenderMode.LIVE) {
            writeLiveSessionMarker(
                File(context.filesDir, "inbox"), System.currentTimeMillis(), listenPort,
            )
        }
        context.startActivity(
            Intent(context, HawkeyeActivity::class.java)
                .putExtra(EXTRA_MODE, mode.name)
                .apply { deviceIp?.let { putExtra(EXTRA_DEVICE_IP, it) } },
        )
    }
}

/**
 * Write the live-session marker the native renderer reads at startup: the millis token and the
 * chosen listen port as "<millis> <port>", matching the C `read_live_marker` parser (a bare
 * token without the port remains valid and falls back to the default).
 */
internal fun writeLiveSessionMarker(inboxDir: File, tokenMillis: Long, listenPort: Int) {
    inboxDir.mkdirs()
    File(inboxDir, ".live").writeText("$tokenMillis $listenPort")
}
