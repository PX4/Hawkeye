package com.px4.hawkeye.android.render.swarm

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.px4.hawkeye.android.render.SwarmWheelSnapshot
import com.px4.hawkeye.core.designsystem.HawkeyeDronePalette
import com.px4.hawkeye.core.presentation.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SwarmWheelViewModelTest {

    // Shared scheduler so the VM's viewModelScope poll loop can be advanced deterministically.
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = UnconfinedTestDispatcher(scheduler)

    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    private fun openSnapshot(fingerX: Float = 50f) = SwarmWheelSnapshot(
        phase = SwarmWheelSnapshot.PHASE_OPEN,
        centerX = 100f, centerY = 200f,
        fingerX = fingerX, fingerY = 60f,
        releaseSeq = 0, releaseX = 0f, releaseY = 0f,
        droneCount = 2, selected = 0,
    )

    @Test
    fun `self-polls the controller on the idle cadence`() {
        val controller = FakeSwarmController().apply {
            snapshot = SwarmWheelSnapshot.Idle.copy(droneCount = 2)
        }
        val vm = SwarmWheelViewModel(controller, droneLabels = listOf("a.ulg", "b.ulg"))
        // The init loop runs one eager poll at construction.
        assertThat(vm.state.value.gesture.droneCount).isEqualTo(2)

        // While idle, a change is picked up on the slow tick, not before.
        controller.snapshot = controller.snapshot.copy(droneCount = 3)
        scheduler.advanceTimeBy(SwarmWheelViewModel.ACTIVE_POLL_MS + 1)
        assertThat(vm.state.value.gesture.droneCount).isEqualTo(2)
        scheduler.advanceTimeBy(SwarmWheelViewModel.IDLE_POLL_MS + 1)
        assertThat(vm.state.value.gesture.droneCount).isEqualTo(3)
    }

    @Test
    fun `polls on the fast cadence while the gesture is active`() {
        val controller = FakeSwarmController().apply { snapshot = openSnapshot(fingerX = 10f) }
        val vm = SwarmWheelViewModel(controller, droneLabels = listOf("a.ulg", "b.ulg"))

        controller.snapshot = openSnapshot(fingerX = 99f)
        scheduler.advanceTimeBy(SwarmWheelViewModel.ACTIVE_POLL_MS + 1)

        assertThat(vm.state.value.gesture.fingerX).isEqualTo(99f)
    }

    @Test
    fun `slice labels are numbered to match the renderer's drone indicators`() {
        val controller = FakeSwarmController().apply {
            snapshot = SwarmWheelSnapshot.Idle.copy(droneCount = 2)
        }
        val vm = SwarmWheelViewModel(controller, droneLabels = listOf("alpha.ulg", "bravo.ulg"))

        val items = vm.state.value.items
        // StringResource carries no structural equality; compare fields per item.
        items.forEachIndexed { index, item ->
            val label = item.label as UiText.StringResource
            assertThat(label.id).isEqualTo(com.px4.hawkeye.android.R.string.swarm_drone_label)
            assertThat(label.args.toList()).isEqualTo(listOf<Any>(index + 1))
        }
        assertThat(items.map { it.accentColor }).containsExactly(
            HawkeyeDronePalette.colors[0],
            HawkeyeDronePalette.colors[1],
        )
    }

    @Test
    fun `hub labels carry the staged file names`() {
        val controller = FakeSwarmController().apply {
            snapshot = SwarmWheelSnapshot.Idle.copy(droneCount = 2)
        }
        val vm = SwarmWheelViewModel(controller, droneLabels = listOf("alpha.ulg", "bravo.ulg"))

        assertThat(vm.state.value.items.map { it.hubLabel }).containsExactly(
            UiText.DynamicString("alpha.ulg"),
            UiText.DynamicString("bravo.ulg"),
        )
    }

    @Test
    fun `long file names are truncated with an ellipsis for the hub`() {
        val controller = FakeSwarmController().apply {
            snapshot = SwarmWheelSnapshot.Idle.copy(droneCount = 3)
        }
        val vm = SwarmWheelViewModel(
            controller,
            droneLabels = listOf(
                "flight_2026_05_28_long_name.ulg", // far past the cap: truncated
                "exactly12chr",                    // at the cap: untouched
                "thirteenchars",                   // one past the cap: truncated
            ),
        )

        assertThat(vm.state.value.items.map { it.hubLabel }).containsExactly(
            UiText.DynamicString("flight_2026_…"),
            UiText.DynamicString("exactly12chr"),
            UiText.DynamicString("thirteenchar…"),
        )
    }

    @Test
    fun `missing or blank file names fall back to the numbered drone name in the hub`() {
        val controller = FakeSwarmController().apply {
            snapshot = SwarmWheelSnapshot.Idle.copy(droneCount = 3)
        }
        val vm = SwarmWheelViewModel(controller, droneLabels = listOf("alpha.ulg", " "))

        val hubLabels = vm.state.value.items.map { it.hubLabel }
        assertThat(hubLabels[0]).isEqualTo(UiText.DynamicString("alpha.ulg"))
        val blank = hubLabels[1] as UiText.StringResource
        assertThat(blank.id).isEqualTo(com.px4.hawkeye.android.R.string.swarm_drone_label)
        assertThat(blank.args.toList()).isEqualTo(listOf<Any>(2))
        val missing = hubLabels[2] as UiText.StringResource
        assertThat(missing.id).isEqualTo(com.px4.hawkeye.android.R.string.swarm_drone_label)
        assertThat(missing.args.toList()).isEqualTo(listOf<Any>(3))
    }

    @Test
    fun `items truncate to the native drone count`() {
        val controller = FakeSwarmController().apply {
            snapshot = SwarmWheelSnapshot.Idle.copy(droneCount = 1)
        }
        val vm = SwarmWheelViewModel(controller, droneLabels = listOf("a.ulg", "b.ulg", "c.ulg"))

        assertThat(vm.state.value.items).hasSize(1)
    }

    @Test
    fun `selecting a drone forwards the index to the controller`() {
        val controller = FakeSwarmController().apply { snapshot = openSnapshot() }
        val vm = SwarmWheelViewModel(controller, droneLabels = listOf("a.ulg", "b.ulg"))

        vm.onAction(SwarmWheelAction.OnDroneSelected(1))

        assertThat(controller.selectedIndices).containsExactly(1)
    }
}
