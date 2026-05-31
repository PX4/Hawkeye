package com.px4.hawkeye.feature.home.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.px4.hawkeye.core.domain.DataError
import com.px4.hawkeye.core.domain.LibraryEntry
import com.px4.hawkeye.core.domain.Result
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
    private lateinit var repo: FakeReplayLibraryRepository

    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher); repo = FakeReplayLibraryRepository() }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `replay click emits NavigateToReplay`() = runTest {
        val vm = HomeViewModel(repo)
        vm.events.test {
            vm.onAction(HomeAction.OnReplayClicked)
            assertThat(awaitItem()).isEqualTo(HomeEvent.NavigateToReplay)
        }
    }

    @Test
    fun `connect click emits NavigateToLive`() = runTest {
        val vm = HomeViewModel(repo)
        vm.events.test {
            vm.onAction(HomeAction.OnConnectClicked)
            assertThat(awaitItem()).isEqualTo(HomeEvent.NavigateToLive)
        }
    }

    @Test
    fun `library exposes at most three recents in order`() = runTest {
        repo.entriesFlow.value = listOf(
            LibraryEntry("1", "a.ulg", 1L, 4L),
            LibraryEntry("2", "b.ulg", 1L, 3L),
            LibraryEntry("3", "c.ulg", 1L, 2L),
            LibraryEntry("4", "d.ulg", 1L, 1L),
        )
        val vm = HomeViewModel(repo)
        vm.state.test {
            assertThat(awaitItem().recents.map { it.id }).containsExactly("1", "2", "3")
        }
    }

    @Test
    fun `recent click stages then emits PlayRecent`() = runTest {
        repo.entriesFlow.value = listOf(LibraryEntry("1", "a.ulg", 1L, 0L))
        val vm = HomeViewModel(repo)
        vm.events.test {
            vm.onAction(HomeAction.OnRecentClicked("1"))
            assertThat(awaitItem()).isEqualTo(HomeEvent.PlayRecent("1"))
        }
        assertThat(repo.stagedIds).containsExactly("1")
    }

    @Test
    fun `recent click stage failure emits ShowError`() = runTest {
        repo.stageResult = Result.Error(DataError.Local.NOT_FOUND)
        val vm = HomeViewModel(repo)
        vm.events.test {
            vm.onAction(HomeAction.OnRecentClicked("1"))
            assertThat(awaitItem()).isInstanceOf(HomeEvent.ShowError::class)
        }
    }
}
