package com.px4.hawkeye.feature.about.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
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
class AboutViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `exposes the version name from the provider`() = runTest {
        val vm = AboutViewModel(FakeAboutInfoProvider(versionName = "0.4.0"))
        assertThat(vm.state.value.versionName).isEqualTo("0.4.0")
    }

    @Test
    fun `loads notices on init`() = runTest {
        val vm = AboutViewModel(FakeAboutInfoProvider(notices = "raylib zlib license"))
        assertThat(vm.state.value.notices).isEqualTo("raylib zlib license")
    }

    @Test
    fun `keeps notices null when the assets cannot be read`() = runTest {
        val vm = AboutViewModel(FakeAboutInfoProvider(failNotices = true))
        assertThat(vm.state.value.notices).isNull()
    }

    @Test
    fun `still reports the version when notices fail to load`() = runTest {
        val vm = AboutViewModel(FakeAboutInfoProvider(versionName = "0.4.0", failNotices = true))
        assertThat(vm.state.value.versionName).isEqualTo("0.4.0")
    }

    @Test
    fun `licenses start collapsed`() = runTest {
        val vm = AboutViewModel(FakeAboutInfoProvider())
        assertThat(vm.state.value.isLicensesExpanded).isFalse()
    }

    @Test
    fun `toggle expands then collapses the licenses`() = runTest {
        val vm = AboutViewModel(FakeAboutInfoProvider())

        vm.onAction(AboutAction.OnToggleLicenses)
        assertThat(vm.state.value.isLicensesExpanded).isTrue()

        vm.onAction(AboutAction.OnToggleLicenses)
        assertThat(vm.state.value.isLicensesExpanded).isFalse()
    }
}
