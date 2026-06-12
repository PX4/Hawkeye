package com.px4.hawkeye.android.render

/**
 * Snapshot of the native wheel-gesture state machine plus the swarm session shape, as
 * published by the render thread each frame (see `swarm_control.h`). Coordinates are in
 * window-surface pixels, the same space the full-screen wheel overlay draws in.
 */
data class SwarmWheelSnapshot(
    val phase: Int,
    val centerX: Float,
    val centerY: Float,
    val fingerX: Float,
    val fingerY: Float,
    /** Bumped once per release-while-open; [releaseX]/[releaseY] hold that finger position. */
    val releaseSeq: Int,
    val releaseX: Float,
    val releaseY: Float,
    val droneCount: Int,
    val selected: Int,
) {
    companion object {
        // Mirrors wheel_phase_t in src/wheel_gesture.h.
        const val PHASE_IDLE = 0
        const val PHASE_PENDING = 1
        const val PHASE_OPEN = 2
        const val PHASE_REJECTED = 3

        val Idle = SwarmWheelSnapshot(
            phase = PHASE_IDLE,
            centerX = 0f, centerY = 0f,
            fingerX = 0f, fingerY = 0f,
            releaseSeq = 0, releaseX = 0f, releaseY = 0f,
            droneCount = 0, selected = 0,
        )
    }
}

/**
 * Controls drone selection in the native renderer. The implementation talks to C via JNI;
 * tests use a fake. Calls are marshalled to the render thread through a lock-free control
 * surface (see `swarm_control.h`).
 */
interface SwarmController {
    fun wheel(): SwarmWheelSnapshot
    fun selectDrone(index: Int)
}

/** JNI-backed [SwarmController]. Only used inside the `:renderer` process. */
class NativeSwarmController : SwarmController {

    override fun wheel(): SwarmWheelSnapshot {
        val s = nativeGetWheel()
        // [phase, centerX, centerY, fingerX, fingerY, releaseSeq, releaseX, releaseY,
        //  droneCount, selected]
        if (s.size < 10) return SwarmWheelSnapshot.Idle
        return SwarmWheelSnapshot(
            phase = s[0].toInt(),
            centerX = s[1],
            centerY = s[2],
            fingerX = s[3],
            fingerY = s[4],
            releaseSeq = s[5].toInt(),
            releaseX = s[6],
            releaseY = s[7],
            droneCount = s[8].toInt(),
            selected = s[9].toInt(),
        )
    }

    override fun selectDrone(index: Int) = nativeSelectDrone(index)

    private external fun nativeGetWheel(): FloatArray
    private external fun nativeSelectDrone(index: Int)

    companion object {
        init {
            // Already loaded by native_app_glue when the NativeActivity starts; idempotent.
            System.loadLibrary("hawkeye")
        }
    }
}
