package com.px4.hawkeye.android.render.live

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.px4.hawkeye.android.render.LiveStatus
import com.px4.hawkeye.android.render.LiveStatusController
import com.px4.hawkeye.feature.live.domain.LiveConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class FakeLiveStatusController(var next: LiveStatus) : LiveStatusController {
    var calls = 0
    override fun status(): LiveStatus { calls++; return next }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LiveStatusViewModelTest {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = UnconfinedTestDispatcher(scheduler)
    private val waiting = LiveStatus(LiveConnectionState.WAITING, sysid = 0, port = 19410)

    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test fun `self-polls the controller on a cadence`() {
        val controller = FakeLiveStatusController(waiting)
        val vm = LiveStatusViewModel(controller, deviceIp = "10.0.0.5")
        // The init loop runs one eager poll at construction.
        assertThat(vm.state.value).isEqualTo(waiting)
        val connected = LiveStatus(LiveConnectionState.CONNECTED, sysid = 1, port = 19410)
        controller.next = connected
        scheduler.advanceTimeBy(LiveStatusViewModel.POLL_INTERVAL_MS + 1)
        assertThat(vm.state.value).isEqualTo(connected)
    }

    @Test fun `surfaces the supplied device ip`() {
        val vm = LiveStatusViewModel(FakeLiveStatusController(waiting), deviceIp = "192.168.1.42")
        assertThat(vm.deviceIp).isEqualTo("192.168.1.42")
    }

    @Test fun `null device ip is surfaced`() {
        val vm = LiveStatusViewModel(FakeLiveStatusController(waiting), deviceIp = null)
        assertThat(vm.deviceIp).isNull()
    }

    @Test fun `refresh reads the controller snapshot into state`() {
        val controller = FakeLiveStatusController(waiting)
        val vm = LiveStatusViewModel(controller, deviceIp = "10.0.0.5")

        vm.refresh()
        assertThat(vm.state.value).isEqualTo(waiting)

        controller.next = LiveStatus(LiveConnectionState.CONNECTED, sysid = 1, port = 19410)
        vm.refresh()
        assertThat(vm.state.value).isEqualTo(controller.next)
    }
}
