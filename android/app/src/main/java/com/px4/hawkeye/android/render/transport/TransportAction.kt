package com.px4.hawkeye.android.render.transport

sealed interface TransportAction {
    data object OnPlayPause : TransportAction
    /** [fraction] is 0..1 along the timeline. */
    data class OnSeek(val fraction: Float) : TransportAction
    data object OnCycleSpeed : TransportAction
}
