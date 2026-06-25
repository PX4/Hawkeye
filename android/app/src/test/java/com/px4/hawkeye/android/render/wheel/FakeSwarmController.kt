package com.px4.hawkeye.android.render.wheel

import com.px4.hawkeye.android.render.SwarmController
import com.px4.hawkeye.android.render.SwarmWheelSnapshot

class FakeSwarmController : SwarmController {
    var snapshot: SwarmWheelSnapshot = SwarmWheelSnapshot.Idle

    val selectedIndices = mutableListOf<Int>()

    override fun wheel(): SwarmWheelSnapshot = snapshot

    override fun selectDrone(index: Int) {
        selectedIndices += index
    }
}
