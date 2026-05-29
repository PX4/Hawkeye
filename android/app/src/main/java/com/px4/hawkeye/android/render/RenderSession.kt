package com.px4.hawkeye.android.render

/**
 * Typed description of what the renderer should show, passed from the shell to
 * [com.px4.hawkeye.android.HawkeyeActivity] as intent extras. Encoded as a plain
 * String map so it maps 1:1 onto Intent extras and is unit-testable without Android.
 */
sealed interface RenderSession {
    data class Replay(val filePath: String) : RenderSession
    data class Live(val host: String, val port: Int) : RenderSession

    fun toExtras(): Map<String, String> = when (this) {
        is Replay -> mapOf(KEY_MODE to MODE_REPLAY, KEY_PATH to filePath)
        is Live -> mapOf(KEY_MODE to MODE_LIVE, KEY_HOST to host, KEY_PORT to port.toString())
    }

    companion object {
        const val KEY_MODE = "com.px4.hawkeye.render.mode"
        const val KEY_PATH = "com.px4.hawkeye.render.path"
        const val KEY_HOST = "com.px4.hawkeye.render.host"
        const val KEY_PORT = "com.px4.hawkeye.render.port"
        const val MODE_REPLAY = "replay"
        const val MODE_LIVE = "live"

        fun fromExtras(extras: Map<String, String?>): RenderSession? =
            when (extras[KEY_MODE]) {
                MODE_REPLAY -> extras[KEY_PATH]?.let { Replay(it) }
                MODE_LIVE -> {
                    val host = extras[KEY_HOST]
                    val port = extras[KEY_PORT]?.toIntOrNull()
                    if (host != null && port != null) Live(host, port) else null
                }
                else -> null
            }
    }
}
