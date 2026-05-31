package com.px4.hawkeye.feature.replay.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import com.px4.hawkeye.core.domain.DataError
import com.px4.hawkeye.core.domain.Result
import com.px4.hawkeye.core.domain.LibraryEntry
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
class ReplayLibraryViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: FakeReplayLibraryRepository

    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher); repo = FakeReplayLibraryRepository() }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `observing library populates entries and clears loading`() = runTest {
        repo.entriesFlow.value = listOf(LibraryEntry("1", "a.ulg", 2048L, 0L))
        val vm = ReplayLibraryViewModel(repo)

        vm.state.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.entries.map { it.id }).containsExactly("1")
        }
    }

    @Test
    fun `observing library lists every entry in repository order`() = runTest {
        repo.entriesFlow.value = listOf(
            LibraryEntry("1", "newest.ulg", 1L, 30L),
            LibraryEntry("2", "middle.ulg", 1L, 20L),
            LibraryEntry("3", "oldest.ulg", 1L, 10L),
        )
        val vm = ReplayLibraryViewModel(repo)

        vm.state.test {
            val entries = awaitItem().entries
            assertThat(entries.map { it.id }).containsExactly("1", "2", "3")
            assertThat(entries.map { it.displayName })
                .containsExactly("newest.ulg", "middle.ulg", "oldest.ulg")
        }
    }

    @Test
    fun `OnOpenFileClicked emits LaunchFilePicker`() = runTest {
        val vm = ReplayLibraryViewModel(repo)

        vm.events.test {
            vm.onAction(ReplayLibraryAction.OnOpenFileClicked)
            assertThat(awaitItem()).isEqualTo(ReplayLibraryEvent.LaunchFilePicker)
        }
    }

    @Test
    fun `OnFilePicked with a uri imports it`() = runTest {
        repo.importResult = Result.Success(LibraryEntry("x", "x.ulg", 1L, 0L))
        val vm = ReplayLibraryViewModel(repo)

        vm.events.test {
            vm.onAction(ReplayLibraryAction.OnFilePicked("content://doc"))
            expectNoEvents()
        }
        assertThat(repo.importedUris).containsExactly("content://doc")
    }

    @Test
    fun `import failure emits ShowError`() = runTest {
        repo.importResult = Result.Error(DataError.Local.DISK_FULL)
        val vm = ReplayLibraryViewModel(repo)

        vm.events.test {
            vm.onAction(ReplayLibraryAction.OnFilePicked("content://doc"))
            assertThat(awaitItem()).isInstanceOf(ReplayLibraryEvent.ShowError::class)
        }
    }

    @Test
    fun `OnFilePicked with null uri does nothing`() = runTest {
        val vm = ReplayLibraryViewModel(repo)

        vm.onAction(ReplayLibraryAction.OnFilePicked(null))

        assertThat(repo.importedUris).isEqualTo(emptyList())
    }

    @Test
    fun `OnEntryClicked stages then emits LaunchReplay`() = runTest {
        val vm = ReplayLibraryViewModel(repo)

        vm.events.test {
            vm.onAction(ReplayLibraryAction.OnEntryClicked("42"))
            assertThat(awaitItem()).isEqualTo(ReplayLibraryEvent.LaunchReplay("42"))
        }
        assertThat(repo.stagedIds).containsExactly("42")
    }

    @Test
    fun `stage failure emits ShowError and not LaunchReplay`() = runTest {
        repo.stageResult = Result.Error(DataError.Local.NOT_FOUND)
        val vm = ReplayLibraryViewModel(repo)

        vm.events.test {
            vm.onAction(ReplayLibraryAction.OnEntryClicked("42"))
            assertThat(awaitItem()).isInstanceOf(ReplayLibraryEvent.ShowError::class)
        }
    }

    @Test
    fun `delete request then confirm deletes and clears the pending entry`() = runTest {
        repo.entriesFlow.value = listOf(LibraryEntry("1", "a.ulg", 1L, 0L))
        val vm = ReplayLibraryViewModel(repo)

        vm.onAction(ReplayLibraryAction.OnDeleteRequested("1"))
        assertThat(vm.state.value.pendingDelete?.id).isEqualTo("1")

        vm.onAction(ReplayLibraryAction.OnConfirmDelete)

        assertThat(vm.state.value.pendingDelete).isNull()
        assertThat(repo.deletedIds).containsExactly("1")
        assertThat(vm.state.value.entries).isEqualTo(emptyList())
    }

    @Test
    fun `delete request then dismiss does not delete`() = runTest {
        repo.entriesFlow.value = listOf(LibraryEntry("1", "a.ulg", 1L, 0L))
        val vm = ReplayLibraryViewModel(repo)

        vm.onAction(ReplayLibraryAction.OnDeleteRequested("1"))
        vm.onAction(ReplayLibraryAction.OnDismissDelete)

        assertThat(vm.state.value.pendingDelete).isNull()
        assertThat(repo.deletedIds).isEqualTo(emptyList())
    }
}
