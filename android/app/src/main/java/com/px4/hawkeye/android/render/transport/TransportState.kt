package com.px4.hawkeye.android.render.transport

import androidx.compose.runtime.Stable

@Stable
data class TransportState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
)
