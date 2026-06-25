package com.px4.hawkeye.android.render.wheel

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.px4.hawkeye.android.R
import com.px4.hawkeye.android.render.SwarmWheelSnapshot
import com.px4.hawkeye.android.render.ViewSnapshot
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
class RootWheelViewModelTest {

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

    private fun viewModel(
        droneCount: Int = 2,
        view: ViewSnapshot = ViewSnapshot.Default,
        droneLabels: List<String> = listOf("a.ulg", "b.ulg"),
    ): RootWheelViewModel {
        val swarm = FakeSwarmController().apply {
            snapshot = SwarmWheelSnapshot.Idle.copy(droneCount = droneCount)
        }
        val views = FakeViewController().apply { snapshot = view }
        return RootWheelViewModel(swarm, views, droneLabels)
    }

    private fun RootWheelViewModel.root() = state.value.root
    private fun RootWheelViewModel.changeViewChildren() = root()[0].children!!
    private fun RootWheelViewModel.selectDroneChildren() = root()[1].children!!

    // --- root shape ------------------------------------------------------------------

    @Test
    fun `root branches show short ring labels with the full phrase in the hub`() {
        val vm = viewModel(droneCount = 2)
        val labelIds = vm.root().map { (it.label as UiText.StringResource).id }
        assertThat(labelIds).containsExactly(R.string.wheel_views, R.string.wheel_drones)
        val hubIds = vm.root().map { (it.hubLabel as UiText.StringResource).id }
        assertThat(hubIds).containsExactly(R.string.wheel_change_view, R.string.wheel_select_drone)
    }

    @Test
    fun `single-drone session shows the view types directly, no extra level`() {
        val vm = viewModel(droneCount = 1, droneLabels = listOf("a.ulg"))
        assertThat(vm.root().map { it.id }).containsExactly(
            "view:cam:0", "view:cam:1", "view:cam:2",
            "view:ortho:1", "view:ortho:2", "view:ortho:3",
            "view:ortho:4", "view:ortho:5", "view:ortho:6",
            "view:panel",
        )
    }

    @Test
    fun `Change View lists the three cameras, six ortho views, and the panel toggle`() {
        val vm = viewModel()
        assertThat(vm.changeViewChildren().map { it.id }).containsExactly(
            "view:cam:0", "view:cam:1", "view:cam:2",
            "view:ortho:1", "view:ortho:2", "view:ortho:3",
            "view:ortho:4", "view:ortho:5", "view:ortho:6",
            "view:panel",
        )
    }

    @Test
    fun `view leaves are leaves, the branches are parents`() {
        val vm = viewModel()
        assertThat(vm.root()[0].children!!).hasSize(10)          // Change View is a parent
        assertThat(vm.changeViewChildren().all { it.children == null }).isTrue()
    }

    // --- active-view highlight -------------------------------------------------------

    @Test
    fun `the default free-track view marks the Free camera active`() {
        val vm = viewModel(view = ViewSnapshot.Default) // CAM_FREE, ORTHO_NONE
        val active = vm.changeViewChildren().filter { it.isActive }.map { it.id }
        assertThat(active).containsExactly("view:cam:2")
    }

    @Test
    fun `an ortho view marks only that ortho slice active`() {
        val vm = viewModel(
            view = ViewSnapshot(ViewSnapshot.CAM_FREE, ViewSnapshot.ORTHO_TOP, panelVisible = false),
        )
        val active = vm.changeViewChildren().filter { it.isActive }.map { it.id }
        assertThat(active).containsExactly("view:ortho:1")
    }

    @Test
    fun `the panel toggle is active while the side panel is visible`() {
        val vm = viewModel(
            view = ViewSnapshot(ViewSnapshot.CAM_CHASE, ViewSnapshot.ORTHO_NONE, panelVisible = true),
        )
        val panel = vm.changeViewChildren().first { it.id == "view:panel" }
        val chase = vm.changeViewChildren().first { it.id == "view:cam:0" }
        assertThat(panel.isActive).isTrue()
        assertThat(chase.isActive).isTrue() // perspective + chase
    }

    // --- drone slices (now nested under Select Drone) --------------------------------

    @Test
    fun `drone slices are numbered and palette-colored to match the renderer`() {
        val vm = viewModel(droneLabels = listOf("alpha.ulg", "bravo.ulg"))
        val drones = vm.selectDroneChildren()
        drones.forEachIndexed { index, item ->
            val label = item.label as UiText.StringResource
            assertThat(label.id).isEqualTo(R.string.swarm_drone_label)
            assertThat(label.args.toList()).isEqualTo(listOf<Any>(index + 1))
            assertThat(item.id).isEqualTo("drone:$index")
        }
        assertThat(drones.map { it.accentColor }).containsExactly(
            HawkeyeDronePalette.colors[0],
            HawkeyeDronePalette.colors[1],
        )
    }

    @Test
    fun `drone hub labels carry the staged file names, truncated when long`() {
        val vm = viewModel(
            droneCount = 3,
            droneLabels = listOf(
                "flight_2026_05_28_long_name.ulg", // far past the cap: truncated
                "exactly12chr",                    // at the cap: untouched
                "thirteenchars",                   // one past the cap: truncated
            ),
        )
        assertThat(vm.selectDroneChildren().map { it.hubLabel }).containsExactly(
            UiText.DynamicString("flight_2026_…"),
            UiText.DynamicString("exactly12chr"),
            UiText.DynamicString("thirteenchar…"),
        )
    }

    @Test
    fun `blank or missing drone names fall back to the numbered name in the hub`() {
        val vm = viewModel(droneCount = 3, droneLabels = listOf("alpha.ulg", " "))
        val hubLabels = vm.selectDroneChildren().map { it.hubLabel }
        assertThat(hubLabels[0]).isEqualTo(UiText.DynamicString("alpha.ulg"))
        val blank = hubLabels[1] as UiText.StringResource
        assertThat(blank.args.toList()).isEqualTo(listOf<Any>(2))
        val missing = hubLabels[2] as UiText.StringResource
        assertThat(missing.args.toList()).isEqualTo(listOf<Any>(3))
    }

    // --- action routing --------------------------------------------------------------

    @Test
    fun `selecting a camera leaf forwards the mode to the view controller`() {
        val swarm = FakeSwarmController().apply { snapshot = openSnapshot() }
        val views = FakeViewController()
        val vm = RootWheelViewModel(swarm, views, listOf("a.ulg", "b.ulg"))

        vm.onAction(RootWheelAction.OnNodeSelected("view:cam:1"))

        assertThat(views.camModes).containsExactly(ViewSnapshot.CAM_FPV)
    }

    @Test
    fun `selecting an ortho leaf forwards the ortho mode`() {
        val swarm = FakeSwarmController()
        val views = FakeViewController()
        val vm = RootWheelViewModel(swarm, views, emptyList())

        vm.onAction(RootWheelAction.OnNodeSelected("view:ortho:3"))

        assertThat(views.orthoModes).containsExactly(ViewSnapshot.ORTHO_FRONT)
    }

    @Test
    fun `selecting the panel leaf toggles the side panel`() {
        val swarm = FakeSwarmController()
        val views = FakeViewController()
        val vm = RootWheelViewModel(swarm, views, emptyList())

        vm.onAction(RootWheelAction.OnNodeSelected("view:panel"))

        assertThat(views.panelToggles).isEqualTo(1)
    }

    @Test
    fun `selecting a drone leaf forwards the index to the swarm controller`() {
        val swarm = FakeSwarmController().apply { snapshot = openSnapshot() }
        val views = FakeViewController()
        val vm = RootWheelViewModel(swarm, views, listOf("a.ulg", "b.ulg"))

        vm.onAction(RootWheelAction.OnNodeSelected("drone:1"))

        assertThat(swarm.selectedIndices).containsExactly(1)
    }

    @Test
    fun `selecting an unknown id touches no controller`() {
        val swarm = FakeSwarmController()
        val views = FakeViewController()
        val vm = RootWheelViewModel(swarm, views, emptyList())

        vm.onAction(RootWheelAction.OnNodeSelected("nope"))

        assertThat(swarm.selectedIndices).isEmpty()
        assertThat(views.camModes).isEmpty()
        assertThat(views.orthoModes).isEmpty()
        assertThat(views.panelToggles).isEqualTo(0)
    }

    // --- polling cadence (unchanged behavior, now over the tree) ---------------------

    @Test
    fun `self-polls the controllers on the idle cadence`() {
        val swarm = FakeSwarmController().apply {
            snapshot = SwarmWheelSnapshot.Idle.copy(droneCount = 2)
        }
        val vm = RootWheelViewModel(swarm, FakeViewController(), listOf("a.ulg", "b.ulg"))
        assertThat(vm.state.value.gesture.droneCount).isEqualTo(2)

        swarm.snapshot = swarm.snapshot.copy(droneCount = 3)
        scheduler.advanceTimeBy(RootWheelViewModel.ACTIVE_POLL_MS + 1)
        assertThat(vm.state.value.gesture.droneCount).isEqualTo(2)
        scheduler.advanceTimeBy(RootWheelViewModel.IDLE_POLL_MS + 1)
        assertThat(vm.state.value.gesture.droneCount).isEqualTo(3)
    }

    @Test
    fun `polls on the fast cadence while the gesture is active`() {
        val swarm = FakeSwarmController().apply { snapshot = openSnapshot(fingerX = 10f) }
        val vm = RootWheelViewModel(swarm, FakeViewController(), listOf("a.ulg", "b.ulg"))

        swarm.snapshot = openSnapshot(fingerX = 99f)
        scheduler.advanceTimeBy(RootWheelViewModel.ACTIVE_POLL_MS + 1)

        assertThat(vm.state.value.gesture.fingerX).isEqualTo(99f)
    }

    @Test
    fun `idle session with no drones shows the view types directly`() {
        val vm = viewModel(droneCount = 0, droneLabels = emptyList())
        assertThat(vm.root().map { it.id }).containsExactly(
            "view:cam:0", "view:cam:1", "view:cam:2",
            "view:ortho:1", "view:ortho:2", "view:ortho:3",
            "view:ortho:4", "view:ortho:5", "view:ortho:6",
            "view:panel",
        )
    }

    @Test
    fun `view leaves carry no hub label so the hub echoes the view name`() {
        val vm = viewModel()
        assertThat(vm.changeViewChildren().mapNotNull { it.hubLabel }).isEmpty()
    }

    @Test
    fun `perspective cameras are inactive while an ortho view is selected`() {
        val vm = viewModel(
            view = ViewSnapshot(ViewSnapshot.CAM_CHASE, ViewSnapshot.ORTHO_LEFT, panelVisible = false),
        )
        val cams = vm.changeViewChildren().filter { it.id?.startsWith("view:cam:") == true }
        assertThat(cams.any { it.isActive }).isFalse()
    }
}
