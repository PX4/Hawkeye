package com.px4.hawkeye.feature.settings.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.px4.hawkeye.feature.settings.domain.DistanceUnit
import com.px4.hawkeye.feature.settings.domain.ThemeMode
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
class SettingsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: FakeSettingsRepository

    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher); repo = FakeSettingsRepository() }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `state reflects repository settings`() = runTest {
        val vm = SettingsViewModel(repo)
        vm.state.test { assertThat(awaitItem().themeMode).isEqualTo(ThemeMode.SYSTEM) }
    }

    @Test
    fun `selecting dark theme updates state`() = runTest {
        val vm = SettingsViewModel(repo)
        vm.state.test {
            awaitItem()
            vm.onAction(SettingsAction.OnThemeModeSelected(ThemeMode.DARK))
            assertThat(awaitItem().themeMode).isEqualTo(ThemeMode.DARK)
        }
    }

    @Test
    fun `selecting imperial units updates state`() = runTest {
        val vm = SettingsViewModel(repo)
        vm.state.test {
            awaitItem()
            vm.onAction(SettingsAction.OnDistanceUnitSelected(DistanceUnit.IMPERIAL))
            assertThat(awaitItem().distanceUnit).isEqualTo(DistanceUnit.IMPERIAL)
        }
    }
}
