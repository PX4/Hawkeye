package com.px4.hawkeye.android.render

import com.px4.hawkeye.feature.live.domain.LiveConnectionState

/** Snapshot of native live MAVLink status. */
data class LiveStatus(
    val state: LiveConnectionState,
    val sysid: Int,
    val port: Int,
)

/**
 * Reads the native live connection snapshot. The implementation talks to C via JNI; tests
 * use a fake. Called from the JVM main thread; the render thread is the sole writer of the
 * underlying atomics (see `replay_control.h`).
 */
interface LiveStatusController {
    fun status(): LiveStatus
}

/** JNI-backed [LiveStatusController]. Only used inside the `:renderer` process. */
class NativeLiveStatusController : LiveStatusController {

    override fun status(): LiveStatus {
        val s = nativeGetLiveStatus()
        // [state, sysid, port]; state 0 = waiting, 1 = connected, 2 = lost.
        if (s.size < 3) return LiveStatus(LiveConnectionState.WAITING, sysid = 0, port = 0)
        val state = when (s[0].toInt()) {
            1 -> LiveConnectionState.CONNECTED
            2 -> LiveConnectionState.LOST
            else -> LiveConnectionState.WAITING
        }
        return LiveStatus(state = state, sysid = s[1].toInt(), port = s[2].toInt())
    }

    private external fun nativeGetLiveStatus(): FloatArray

    companion object {
        init {
            // Already loaded by native_app_glue when the NativeActivity starts; idempotent.
            System.loadLibrary("hawkeye")
        }
    }
}
