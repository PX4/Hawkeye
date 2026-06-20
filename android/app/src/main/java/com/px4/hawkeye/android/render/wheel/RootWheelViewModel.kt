package com.px4.hawkeye.android.render.wheel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.px4.hawkeye.android.R
import com.px4.hawkeye.android.render.SwarmController
import com.px4.hawkeye.android.render.SwarmWheelSnapshot
import com.px4.hawkeye.android.render.ViewController
import com.px4.hawkeye.android.render.ViewSnapshot
import com.px4.hawkeye.core.designsystem.HawkeyeDronePalette
import com.px4.hawkeye.core.presentation.UiText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Backs the root wheel overlay. The wheel is a nested menu: the root offers "Change View"
 * (always) and, with 2+ drones, "Select Drone"; drilling into either reveals its leaves.
 * Like the transport bar, state is a pull-based snapshot of the native engine — the
 * ViewModel owns the polling cadence (fast while the native gesture machine owns a touch so
 * the highlight tracks the finger, relaxed while idle) and [onAction] routes a released leaf
 * to the right controller by its id.
 *
 * (Class/package still named "swarm" for history; it now hosts view-switching too.)
 *
 * [droneLabels] are the staged logs' display names (Intent extra, staged order = drone
 * order); drone slices pair them with the shared palette so the wheel matches the meshes.
 */
class RootWheelViewModel(
    private val swarm: SwarmController,
    private val viewController: ViewController,
    private val droneLabels: List<String>,
) : ViewModel() {

    private val _state = MutableStateFlow(RootWheelState())
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
        val gesture = swarm.wheel()
        val view = viewController.view()
        _state.update { prev ->
            // The menu only depends on the drone count and the active view, so rebuild the
            // (recursive) tree only when one of those changes — not every poll.
            val rebuild = prev.root.isEmpty() ||
                gesture.droneCount != prev.gesture.droneCount ||
                view != prev.view
            prev.copy(
                root = if (rebuild) buildRoot(gesture.droneCount, view) else prev.root,
                gesture = gesture,
                view = view,
            )
        }
    }

    fun onAction(action: RootWheelAction) {
        when (action) {
            is RootWheelAction.OnNodeSelected -> dispatch(action.id)
        }
    }

    private fun dispatch(id: String) = when {
        id.startsWith(ID_CAM_PREFIX) -> viewController.setCamMode(id.substringAfterLast(':').toInt())
        id.startsWith(ID_ORTHO_PREFIX) -> viewController.setOrthoMode(id.substringAfterLast(':').toInt())
        id == ID_PANEL -> viewController.toggleSidePanel()
        id.startsWith(ID_DRONE_PREFIX) -> swarm.selectDrone(id.substringAfterLast(':').toInt())
        else -> Unit
    }

    private fun buildRoot(droneCount: Int, view: ViewSnapshot): List<WheelNodeUi> {
        val accent = HawkeyeDronePalette.colors[0]
        val views = buildViewLeaves(view, accent)

        // A single-drone (or idle) session has nothing to select, so the "Select Drone"
        // branch — and with it the extra root level it would sit beside — is pointless:
        // show the view types directly. Only a swarm nests views under "Change View"
        // alongside "Select Drone".
        if (droneCount < 2) return views

        // Slice labels are short so they fit the ring; the hub echoes the fuller phrase.
        return listOf(
            WheelNodeUi(
                id = null, // structural parent; drilled into, never dispatched
                label = UiText.StringResource(R.string.wheel_views),
                hubLabel = UiText.StringResource(R.string.wheel_change_view),
                accentColor = accent,
                children = views,
            ),
            WheelNodeUi(
                id = null,
                label = UiText.StringResource(R.string.wheel_drones),
                hubLabel = UiText.StringResource(R.string.wheel_select_drone),
                accentColor = accent,
                children = buildDroneItems(droneCount),
            ),
        )
    }

    private fun buildViewLeaves(view: ViewSnapshot, accent: Color): List<WheelNodeUi> {
        val perspective = view.orthoMode == ViewSnapshot.ORTHO_NONE
        return listOf(
            viewLeaf(camId(ViewSnapshot.CAM_CHASE), R.string.wheel_view_chase, accent,
                perspective && view.camMode == ViewSnapshot.CAM_CHASE),
            viewLeaf(camId(ViewSnapshot.CAM_FPV), R.string.wheel_view_fpv, accent,
                perspective && view.camMode == ViewSnapshot.CAM_FPV),
            viewLeaf(camId(ViewSnapshot.CAM_FREE), R.string.wheel_view_free, accent,
                perspective && view.camMode == ViewSnapshot.CAM_FREE),
            viewLeaf(orthoId(ViewSnapshot.ORTHO_TOP), R.string.wheel_view_top, accent,
                view.orthoMode == ViewSnapshot.ORTHO_TOP),
            viewLeaf(orthoId(ViewSnapshot.ORTHO_BOTTOM), R.string.wheel_view_bottom, accent,
                view.orthoMode == ViewSnapshot.ORTHO_BOTTOM),
            viewLeaf(orthoId(ViewSnapshot.ORTHO_FRONT), R.string.wheel_view_front, accent,
                view.orthoMode == ViewSnapshot.ORTHO_FRONT),
            viewLeaf(orthoId(ViewSnapshot.ORTHO_BACK), R.string.wheel_view_back, accent,
                view.orthoMode == ViewSnapshot.ORTHO_BACK),
            viewLeaf(orthoId(ViewSnapshot.ORTHO_LEFT), R.string.wheel_view_left, accent,
                view.orthoMode == ViewSnapshot.ORTHO_LEFT),
            viewLeaf(orthoId(ViewSnapshot.ORTHO_RIGHT), R.string.wheel_view_right, accent,
                view.orthoMode == ViewSnapshot.ORTHO_RIGHT),
            viewLeaf(ID_PANEL, R.string.wheel_view_panel, accent, view.panelVisible),
        )
    }

    private fun viewLeaf(id: String, labelRes: Int, accent: Color, active: Boolean) =
        WheelNodeUi(
            id = id,
            label = UiText.StringResource(labelRes),
            accentColor = accent,
            isActive = active,
        )

    private fun buildDroneItems(count: Int): List<WheelNodeUi> = List(count) { index ->
        // Slices always read "Drone N" so they match the renderer's drone indicators; the
        // staged log's name appears only in the hub, truncated to fit it.
        val numbered = UiText.StringResource(R.string.swarm_drone_label, arrayOf(index + 1))
        val fileName = droneLabels.getOrNull(index)?.takeIf { it.isNotBlank() }
        WheelNodeUi(
            id = droneId(index),
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
         * Longest hub text that fits the wheel hub on one line: the hub is 128 dp across and
         * titleMedium glyphs average ~8 dp, so 12 characters plus the ellipsis stay inside
         * the usable chord with padding to spare.
         */
        const val HUB_LABEL_MAX_CHARS = 12

        // Leaf-id scheme parsed by dispatch(); kept in sync with view_control / scene.h modes.
        private const val ID_CAM_PREFIX = "view:cam:"
        private const val ID_ORTHO_PREFIX = "view:ortho:"
        const val ID_PANEL = "view:panel"
        private const val ID_DRONE_PREFIX = "drone:"

        fun camId(mode: Int) = "$ID_CAM_PREFIX$mode"
        fun orthoId(mode: Int) = "$ID_ORTHO_PREFIX$mode"
        fun droneId(index: Int) = "$ID_DRONE_PREFIX$index"

        private fun truncateForHub(name: String): String =
            if (name.length <= HUB_LABEL_MAX_CHARS) name
            else name.take(HUB_LABEL_MAX_CHARS).trimEnd() + "…"
    }
}
