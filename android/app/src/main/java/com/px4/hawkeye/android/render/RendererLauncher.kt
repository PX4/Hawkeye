package com.px4.hawkeye.android.render

import android.content.Context
import android.content.Intent
import com.px4.hawkeye.android.HawkeyeActivity
import java.io.File

/**
 * Single place the shell uses to launch the native renderer. Replay logs are delivered out
 * of band (the repository stages them into `filesDir/inbox/`). For [RenderMode.LIVE] we drop
 * a `inbox/.live` marker (a fresh millis token) that the native startup compares against the
 * replay `.ready` token (newest wins), and pass the mode as an Intent extra so the Activity
 * can suppress the replay transport bar.
 */
object RendererLauncher {

    const val EXTRA_MODE = "com.px4.hawkeye.android.render.MODE"

    fun launch(context: Context, mode: RenderMode = RenderMode.REPLAY) {
        if (mode == RenderMode.LIVE) {
            writeLiveSessionMarker(File(context.filesDir, "inbox"), System.currentTimeMillis())
        }
        context.startActivity(
            Intent(context, HawkeyeActivity::class.java).putExtra(EXTRA_MODE, mode.name),
        )
    }
}

/**
 * Write the live-session marker the native renderer reads at startup. The content is the
 * millis token as decimal text, matching the C `read_ready_token` parser.
 */
internal fun writeLiveSessionMarker(inboxDir: File, tokenMillis: Long) {
    inboxDir.mkdirs()
    File(inboxDir, ".live").writeText(tokenMillis.toString())
}
