package com.px4.hawkeye.android.render.wheel

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.px4.hawkeye.android.render.SwarmWheelSnapshot
import com.px4.hawkeye.android.render.ViewSnapshot
import com.px4.hawkeye.core.presentation.UiText

@Stable
data class RootWheelState(
    /** Top-level wheel menu (e.g. Change View, Select Drone); empty until first poll. */
    val root: List<WheelNodeUi> = emptyList(),
    /** Latest native gesture/session snapshot driving the wheel widget. */
    val gesture: SwarmWheelSnapshot = SwarmWheelSnapshot.Idle,
    /** Latest active-view snapshot, used to highlight the current view slice. */
    val view: ViewSnapshot = ViewSnapshot.Default,
)

/**
 * App-level wheel node carrying deferred [UiText] strings and submenu structure. Mapped to
 * the design-system `WheelMenuItem` (plain Strings) in the screen, recursively for submenus.
 *
 * @property id Stable action id; null for structural-only parents. Leaves dispatch on it.
 * @property children Non-null for a parent (drilled into on dwell); null for a leaf.
 */
data class WheelNodeUi(
    val id: String?,
    val label: UiText,
    val accentColor: Color,
    val hubLabel: UiText? = null,
    val isActive: Boolean = false,
    val children: List<WheelNodeUi>? = null,
)
