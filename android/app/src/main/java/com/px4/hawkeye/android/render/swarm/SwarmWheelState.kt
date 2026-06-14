package com.px4.hawkeye.android.render.swarm

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.px4.hawkeye.android.render.SwarmWheelSnapshot
import com.px4.hawkeye.core.presentation.UiText

@Stable
data class SwarmWheelState(
    /** One entry per drone, in staged order; empty until a session loads. */
    val items: List<DroneWheelItemUi> = emptyList(),
    /** Latest native gesture/session snapshot driving the wheel widget. */
    val gesture: SwarmWheelSnapshot = SwarmWheelSnapshot.Idle,
)

data class DroneWheelItemUi(
    /** Slice label ("Drone N"), numbered to match the renderer's drone indicators. */
    val label: UiText,
    /** Hub echo while hovered: the staged log's name, truncated to fit the hub. */
    val hubLabel: UiText,
    val accentColor: Color,
)
