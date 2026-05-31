package com.px4.hawkeye.android.render.transport

import com.px4.hawkeye.android.render.ReplayController
import com.px4.hawkeye.android.render.ReplayStatus

class FakeReplayController : ReplayController {
    var status = ReplayStatus(active = true, paused = false, positionS = 0f, durationS = 100f, speed = 1f)
    var pausedArg: Boolean? = null
    var speedArg: Float? = null
    var seekArg: Float? = null

    override fun status() = status
    override fun setPaused(paused: Boolean) { pausedArg = paused }
    override fun setSpeed(speed: Float) { speedArg = speed }
    override fun seekTo(seconds: Float) { seekArg = seconds }
}
