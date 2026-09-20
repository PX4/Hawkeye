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
        val vm = viewModel(ip = "192.168.1.42", listenPort = 14550)
        val state = vm.state.value
        assertThat(state.deviceIp).isEqualTo("192.168.1.42")
        assertThat(state.listenPort).isEqualTo(14550)
        assertThat(state.endpoint).isEqualTo("udp://192.168.1.42:14550")
    }

    @Test
    fun `null ip yields empty endpoint`() = runTest {
        val vm = viewModel(ip = null)
        val state = vm.state.value
        assertThat(state.deviceIp).isNull()
        assertThat(state.endpoint).isEqualTo("")
    }

    @Test
    fun `start click launches straight away when permission is already held`() = runTest {
        val vm = viewModel(permission = FakeLocalNetworkPermission(granted = true))
        vm.events.test {
            vm.onAction(LiveSetupAction.OnStartLiveClicked)
            assertThat(awaitItem()).isSameInstanceAs(LiveSetupEvent.LaunchLiveSession)
        }
    }

    @Test
    fun `start click asks for permission instead of launching when it is missing`() = runTest {
        val vm = viewModel(permission = FakeLocalNetworkPermission(granted = false))
        vm.events.test {
            vm.onAction(LiveSetupAction.OnStartLiveClicked)
            assertThat(awaitItem()).isSameInstanceAs(LiveSetupEvent.RequestLocalNetworkPermission)
            expectNoEvents()
        }
    }

    @Test
    fun `resuming clears the notice once permission was granted elsewhere`() = runTest {
        val permission = FakeLocalNetworkPermission(granted = false)
        val vm = viewModel(permission = permission)
        vm.onAction(LiveSetupAction.OnLocalNetworkPermissionResult(granted = false))
        assertThat(vm.state.value.localNetworkDenied).isTrue()

        // Stands in for the user granting on the system settings page, which reports
        // no result back to the app.
        permission.granted = true
        vm.onAction(LiveSetupAction.OnResumed)
        assertThat(vm.state.value.localNetworkDenied).isFalse()
    }

    @Test
    fun `resuming without permission never invents a refusal`() = runTest {
        val vm = viewModel(permission = FakeLocalNetworkPermission(granted = false))
        vm.events.test {
            vm.onAction(LiveSetupAction.OnResumed)
            expectNoEvents()
        }
        assertThat(vm.state.value.localNetworkDenied).isFalse()
    }

    @Test
    fun `granted local network permission launches the session`() = runTest {
        val vm = viewModel()
        vm.events.test {
            vm.onAction(LiveSetupAction.OnLocalNetworkPermissionResult(granted = true))
            assertThat(awaitItem()).isSameInstanceAs(LiveSetupEvent.LaunchLiveSession)
        }
    }

    // The denial has to be established first: asserting isFalse() on a fresh ViewModel
    // would pass whether or not the grant branch clears anything.
    @Test
    fun `granted local network permission clears a standing denial`() = runTest {
        val vm = viewModel()
        vm.onAction(LiveSetupAction.OnLocalNetworkPermissionResult(granted = false))
        assertThat(vm.state.value.localNetworkDenied).isTrue()

        vm.onAction(LiveSetupAction.OnLocalNetworkPermissionResult(granted = true))
        assertThat(vm.state.value.localNetworkDenied).isFalse()
    }

    @Test
    fun `denied local network permission surfaces on state and launches nothing`() = runTest {
        val vm = viewModel()
        vm.events.test {
            vm.onAction(LiveSetupAction.OnLocalNetworkPermissionResult(granted = false))
            expectNoEvents()
        }
        assertThat(vm.state.value.localNetworkDenied).isTrue()
    }

    @Test
    fun `start click clears a previous denial`() = runTest {
        val vm = viewModel()
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
        val vm = viewModel()
        vm.events.test {
            vm.onAction(LiveSetupAction.OnOpenAppSettingsClicked)
            assertThat(awaitItem()).isSameInstanceAs(LiveSetupEvent.OpenAppSettings)
        }
    }

    private fun viewModel(
        ip: String? = "10.0.0.5",
        permission: FakeLocalNetworkPermission = FakeLocalNetworkPermission(granted = true),
        listenPort: Int = 19410,
    ) = LiveSetupViewModel(FakeDeviceIpProvider(ip), permission, listenPort)
}
