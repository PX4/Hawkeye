package com.px4.hawkeye.android.render.swarm

sealed interface SwarmWheelAction {
    /** The wheel released over a slice: make that drone the camera/HUD target. */
    data class OnDroneSelected(val index: Int) : SwarmWheelAction
}
