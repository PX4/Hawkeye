package com.px4.hawkeye.android.render

import android.content.Context
import android.content.Intent
import com.px4.hawkeye.android.HawkeyeActivity

/**
 * Single place the shell uses to launch the native renderer. The log to play is delivered
 * out of band: the repository stages it into `filesDir/inbox/` and the native poll loop
 * loads it, so the launch itself carries no data.
 */
object RendererLauncher {
    fun launch(context: Context) {
        context.startActivity(Intent(context, HawkeyeActivity::class.java))
    }
}
