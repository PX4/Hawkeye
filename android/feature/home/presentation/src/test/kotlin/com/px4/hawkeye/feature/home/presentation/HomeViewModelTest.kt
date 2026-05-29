package com.px4.hawkeye.feature.home.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
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
class HomeViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `replay click emits NavigateToReplay`() = runTest {
        val vm = HomeViewModel()
        vm.events.test {
            vm.onAction(HomeAction.OnReplayClicked)
            assertThat(awaitItem()).isEqualTo(HomeEvent.NavigateToReplay)
        }
    }

    @Test
    fun `connect click emits NavigateToLive`() = runTest {
        val vm = HomeViewModel()
        vm.events.test {
            vm.onAction(HomeAction.OnConnectClicked)
            assertThat(awaitItem()).isEqualTo(HomeEvent.NavigateToLive)
        }
    }
}
