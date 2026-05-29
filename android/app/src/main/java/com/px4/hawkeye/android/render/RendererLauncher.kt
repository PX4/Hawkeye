package com.px4.hawkeye.android.render

import android.content.Context
import android.content.Intent
import com.px4.hawkeye.android.HawkeyeActivity

/** Single place the shell uses to launch the native renderer for a given session. */
object RendererLauncher {
    fun launch(context: Context, session: RenderSession) {
        val intent = Intent(context, HawkeyeActivity::class.java)
        session.toExtras().forEach { (k, v) -> intent.putExtra(k, v) }
        context.startActivity(intent)
    }
}
