package com.px4.hawkeye.android.render

/** Snapshot of native replay playback state. */
data class ReplayStatus(
    val active: Boolean,
    val paused: Boolean,
    val positionS: Float,
    val durationS: Float,
    val speed: Float,
)

/**
 * Controls the native replay engine. The implementation talks to C via JNI; tests use a
 * fake. All calls are made from the JVM main thread and are marshalled to the render
 * thread through a lock-free control surface (see `replay_control.h`).
 */
interface ReplayController {
    fun status(): ReplayStatus
    fun setPaused(paused: Boolean)
    fun setSpeed(speed: Float)
    fun seekTo(seconds: Float)
}

/** JNI-backed [ReplayController]. Only used inside the `:renderer` process. */
class NativeReplayController : ReplayController {

    override fun status(): ReplayStatus {
        val s = nativeGetStatus()
        // [active, paused, positionS, durationS, speed]
        if (s.size < 5) return ReplayStatus(active = false, paused = false, 0f, 0f, 1f)
        return ReplayStatus(
            active = s[0] != 0f,
            paused = s[1] != 0f,
            positionS = s[2],
            durationS = s[3],
            speed = s[4],
        )
    }

    override fun setPaused(paused: Boolean) = nativeSetPaused(paused)
    override fun setSpeed(speed: Float) = nativeSetSpeed(speed)
    override fun seekTo(seconds: Float) = nativeSeekTo(seconds)

    private external fun nativeGetStatus(): FloatArray
    private external fun nativeSetPaused(paused: Boolean)
    private external fun nativeSetSpeed(speed: Float)
    private external fun nativeSeekTo(seconds: Float)

    companion object {
        init {
            // Already loaded by native_app_glue when the NativeActivity starts; idempotent.
            System.loadLibrary("hawkeye")
        }
    }
}
