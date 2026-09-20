package com.px4.hawkeye.feature.live.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveSetupViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `builds endpoint from device ip and port`() = runTest {
        val vm = LiveSetupViewModel(FakeDeviceIpProvider("192.168.1.42"), listenPort = 14550)
        val state = vm.state.value
        assertThat(state.deviceIp).isEqualTo("192.168.1.42")
        assertThat(state.listenPort).isEqualTo(14550)
        assertThat(state.endpoint).isEqualTo("udp://192.168.1.42:14550")
    }

    @Test
    fun `null ip yields empty endpoint`() = runTest {
        val vm = LiveSetupViewModel(FakeDeviceIpProvider(null), listenPort = 19410)
        val state = vm.state.value
        assertThat(state.deviceIp).isNull()
        assertThat(state.endpoint).isEqualTo("")
    }

    @Test
    fun `start click emits launch event`() = runTest {
        val vm = LiveSetupViewModel(FakeDeviceIpProvider("10.0.0.5"), listenPort = 19410)
        vm.events.test {
            vm.onAction(LiveSetupAction.OnStartLiveClicked)
            assertThat(awaitItem()).isSameInstanceAs(LiveSetupEvent.LaunchLiveSession)
        }
    }

    @Test
    fun `granted local network permission re-emits launch event`() = runTest {
        val vm = LiveSetupViewModel(FakeDeviceIpProvider("10.0.0.5"), listenPort = 19410)
        vm.events.test {
            vm.onAction(LiveSetupAction.OnLocalNetworkPermissionResult(granted = true))
            assertThat(awaitItem()).isSameInstanceAs(LiveSetupEvent.LaunchLiveSession)
        }
    }

    // The denial has to be established first: asserting isFalse() on a fresh ViewModel
    // would pass whether or not the grant branch clears anything.
    @Test
    fun `granted local network permission clears a standing denial`() = runTest {
        val vm = LiveSetupViewModel(FakeDeviceIpProvider("10.0.0.5"), listenPort = 19410)
        vm.onAction(LiveSetupAction.OnLocalNetworkPermissionResult(granted = false))
        assertThat(vm.state.value.localNetworkDenied).isTrue()

        vm.onAction(LiveSetupAction.OnLocalNetworkPermissionResult(granted = true))
        assertThat(vm.state.value.localNetworkDenied).isFalse()
    }

    @Test
    fun `denied local network permission surfaces on state and launches nothing`() = runTest {
        val vm = LiveSetupViewModel(FakeDeviceIpProvider("10.0.0.5"), listenPort = 19410)
        vm.events.test {
            vm.onAction(LiveSetupAction.OnLocalNetworkPermissionResult(granted = false))
            expectNoEvents()
        }
        assertThat(vm.state.value.localNetworkDenied).isTrue()
    }

    @Test
    fun `start click clears a previous denial`() = runTest {
        val vm = LiveSetupViewModel(FakeDeviceIpProvider("10.0.0.5"), listenPort = 19410)
        vm.onAction(LiveSetupAction.OnLocalNetworkPermissionResult(granted = false))
        assertThat(vm.state.value.localNetworkDenied).isTrue()

        vm.events.test {
            vm.onAction(LiveSetupAction.OnStartLiveClicked)
            assertThat(awaitItem()).isSameInstanceAs(LiveSetupEvent.LaunchLiveSession)
        }
        assertThat(vm.state.value.localNetworkDenied).isFalse()
    }

    @Test
    fun `open settings click emits settings event`() = runTest {
        val vm = LiveSetupViewModel(FakeDeviceIpProvider("10.0.0.5"), listenPort = 19410)
        vm.events.test {
            vm.onAction(LiveSetupAction.OnOpenAppSettingsClicked)
            assertThat(awaitItem()).isSameInstanceAs(LiveSetupEvent.OpenAppSettings)
        }
    }
}
