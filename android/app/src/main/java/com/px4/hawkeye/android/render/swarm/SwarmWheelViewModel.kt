package com.px4.hawkeye.android.render.swarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.px4.hawkeye.android.R
import com.px4.hawkeye.android.render.SwarmController
import com.px4.hawkeye.android.render.SwarmWheelSnapshot
import com.px4.hawkeye.core.designsystem.HawkeyeDronePalette
import com.px4.hawkeye.core.presentation.UiText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Backs the swarm drone-selection wheel overlay. Like the transport bar, state is a
 * pull-based snapshot of the native engine: the ViewModel owns the polling cadence (fast
 * while the native gesture machine owns a touch so the highlight tracks the finger,
 * relaxed while idle) and [onAction] forwards the chosen drone to the [controller].
 *
 * [droneLabels] are the staged logs' display names (Intent extra, staged order = drone
 * order); items pair them with the shared drone palette so the wheel matches the meshes
 * and trails the engine draws.
 */
class SwarmWheelViewModel(
    private val controller: SwarmController,
    private val droneLabels: List<String>,
) : ViewModel() {

    private val _state = MutableStateFlow(SwarmWheelState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                val idle = _state.value.gesture.phase == SwarmWheelSnapshot.PHASE_IDLE
                delay(if (idle) IDLE_POLL_MS else ACTIVE_POLL_MS)
            }
        }
    }

    fun refresh() {
        val snapshot = controller.wheel()
        _state.update {
            it.copy(
                items = if (snapshot.droneCount == it.items.size) it.items
                        else buildItems(snapshot.droneCount),
                gesture = snapshot,
            )
        }
    }

    fun onAction(action: SwarmWheelAction) {
        when (action) {
            is SwarmWheelAction.OnDroneSelected -> controller.selectDrone(action.index)
        }
    }

    private fun buildItems(count: Int): List<DroneWheelItemUi> = List(count) { index ->
        // Slices always read "Drone N" so they match the renderer's drone indicators;
        // the staged log's name appears only in the hub, truncated to fit it.
        val numbered = UiText.StringResource(R.string.swarm_drone_label, arrayOf(index + 1))
        val fileName = droneLabels.getOrNull(index)?.takeIf { it.isNotBlank() }
        DroneWheelItemUi(
            label = numbered,
            hubLabel = fileName?.let { UiText.DynamicString(truncateForHub(it)) } ?: numbered,
            accentColor = HawkeyeDronePalette.colors[index % HawkeyeDronePalette.colors.size],
        )
    }

    companion object {
        /** Poll cadence while the native gesture machine owns a touch (PENDING/OPEN). */
        const val ACTIVE_POLL_MS = 16L

        /** Poll cadence while idle: only needs to catch a hold starting. */
        const val IDLE_POLL_MS = 150L

        /**
         * Longest hub text that fits the wheel hub on one line: the hub is 128 dp across
         * and titleMedium glyphs average ~8 dp, so 12 characters plus the ellipsis stay
         * inside the usable chord with padding to spare.
         */
        const val HUB_LABEL_MAX_CHARS = 12

        private fun truncateForHub(name: String): String =
            if (name.length <= HUB_LABEL_MAX_CHARS) name
            else name.take(HUB_LABEL_MAX_CHARS).trimEnd() + "…"
    }
}
