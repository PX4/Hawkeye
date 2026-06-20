package com.px4.hawkeye.android.render.wheel

sealed interface RootWheelAction {
    /**
     * The wheel released over a leaf slice. [id] encodes the action (e.g. "view:cam:1",
     * "view:ortho:2", "view:panel", "drone:3"); the ViewModel routes it to the right
     * controller.
     */
    data class OnNodeSelected(val id: String) : RootWheelAction
}
