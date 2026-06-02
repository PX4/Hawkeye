package com.px4.hawkeye.android.render.transport

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.px4.hawkeye.android.render.ReplayStatus
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
class TransportViewModelTest {

    // Shared scheduler so the VM's viewModelScope poll loop can be advanced deterministically.
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = UnconfinedTestDispatcher(scheduler)

    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `self-polls the controller on a cadence`() {
        val controller = FakeReplayController().apply {
            status = ReplayStatus(active = true, paused = false, positionS = 0f, durationS = 100f, speed = 1f)
        }
        val vm = TransportViewModel(controller)
        // The init loop runs one eager poll at construction.
        assertThat(vm.state.value.isActive).isTrue()
        // A later native change is picked up on the next tick.
        controller.status = controller.status.copy(positionS = 5f)
        scheduler.advanceTimeBy(TransportViewModel.POLL_INTERVAL_MS + 1)
        assertThat(vm.state.value.positionMs).isEqualTo(5_000L)
    }

    @Test
    fun `refresh maps native status into state`() {
        val controller = FakeReplayController().apply {
            status = ReplayStatus(active = true, paused = true, positionS = 12f, durationS = 100f, speed = 2f)
        }
        val vm = TransportViewModel(controller)

        vm.refresh()

        val state = vm.state.value
        assertThat(state.isActive).isTrue()
        assertThat(state.isPaused).isTrue()
        assertThat(state.positionMs).isEqualTo(12_000L)
        assertThat(state.durationMs).isEqualTo(100_000L)
        assertThat(state.speed).isEqualTo(2f)
    }

    @Test
    fun `play pause toggles and calls the controller`() {
        val controller = FakeReplayController().apply {
            status = ReplayStatus(active = true, paused = false, positionS = 0f, durationS = 100f, speed = 1f)
        }
        val vm = TransportViewModel(controller).apply { refresh() }

        vm.onAction(TransportAction.OnPlayPause)

        assertThat(controller.pausedArg).isEqualTo(true)
        assertThat(vm.state.value.isPaused).isTrue()
    }

    @Test
    fun `seek converts the timeline fraction to seconds`() {
        val controller = FakeReplayController().apply {
            status = ReplayStatus(active = true, paused = false, positionS = 0f, durationS = 200f, speed = 1f)
        }
        val vm = TransportViewModel(controller).apply { refresh() }

        vm.onAction(TransportAction.OnSeek(0.5f))

        assertThat(controller.seekArg).isEqualTo(100f)
    }

    @Test
    fun `cycle speed advances through the ladder`() {
        val controller = FakeReplayController().apply {
            status = ReplayStatus(active = true, paused = false, positionS = 0f, durationS = 100f, speed = 1f)
        }
        val vm = TransportViewModel(controller).apply { refresh() }

        vm.onAction(TransportAction.OnCycleSpeed)

        assertThat(controller.speedArg).isEqualTo(2f)
        assertThat(vm.state.value.speed).isEqualTo(2f)
    }

    @Test
    fun `cycle speed wraps from the top back to the slowest`() {
        val controller = FakeReplayController().apply {
            status = ReplayStatus(active = true, paused = false, positionS = 0f, durationS = 100f, speed = 4f)
        }
        val vm = TransportViewModel(controller).apply { refresh() }

        vm.onAction(TransportAction.OnCycleSpeed)

        assertThat(controller.speedArg).isEqualTo(0.5f)
    }

    @Test
    fun `inactive status keeps the bar hidden`() {
        val controller = FakeReplayController().apply {
            status = ReplayStatus(active = false, paused = false, positionS = 0f, durationS = 0f, speed = 1f)
        }
        val vm = TransportViewModel(controller).apply { refresh() }

        assertThat(vm.state.value.isActive).isFalse()
    }
}
