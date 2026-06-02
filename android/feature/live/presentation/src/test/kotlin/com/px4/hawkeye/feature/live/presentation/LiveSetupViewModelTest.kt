package com.px4.hawkeye.feature.live.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
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
}
