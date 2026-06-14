package com.px4.hawkeye.feature.replay.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
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
        repo.entriesFlow.value = listOf(LibraryEntry("42", "flight.ulg", 1L, 0L))
        val vm = ReplayLibraryViewModel(repo)

        vm.events.test {
            vm.onAction(ReplayLibraryAction.OnEntryClicked("42"))
            assertThat(awaitItem())
                .isEqualTo(ReplayLibraryEvent.LaunchReplay(listOf("42"), listOf("flight.ulg")))
        }
        assertThat(repo.stagedBatches).containsExactly(listOf("42"))
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
    fun `toggling selection mode on and off clears the selection`() = runTest {
        repo.entriesFlow.value = listOf(LibraryEntry("1", "a.ulg", 1L, 0L))
        val vm = ReplayLibraryViewModel(repo)

        vm.onAction(ReplayLibraryAction.OnToggleSelectionMode)
        assertThat(vm.state.value.isSelectionMode).isTrue()

        vm.onAction(ReplayLibraryAction.OnEntryClicked("1"))
        assertThat(vm.state.value.selectedIds).containsExactly("1")

        vm.onAction(ReplayLibraryAction.OnToggleSelectionMode)
        assertThat(vm.state.value.isSelectionMode).isFalse()
        assertThat(vm.state.value.selectedIds).isEqualTo(emptyList())
    }

    @Test
    fun `entry clicks in selection mode toggle membership preserving click order`() = runTest {
        repo.entriesFlow.value = listOf(
            LibraryEntry("1", "a.ulg", 1L, 0L),
            LibraryEntry("2", "b.ulg", 1L, 0L),
            LibraryEntry("3", "c.ulg", 1L, 0L),
        )
        val vm = ReplayLibraryViewModel(repo)
        vm.onAction(ReplayLibraryAction.OnToggleSelectionMode)

        vm.onAction(ReplayLibraryAction.OnEntryClicked("2"))
        vm.onAction(ReplayLibraryAction.OnEntryClicked("1"))
        vm.onAction(ReplayLibraryAction.OnEntryClicked("3"))
        assertThat(vm.state.value.selectedIds).containsExactly("2", "1", "3")

        vm.onAction(ReplayLibraryAction.OnEntryClicked("1"))
        assertThat(vm.state.value.selectedIds).containsExactly("2", "3")
        assertThat(repo.stagedBatches).isEqualTo(emptyList<List<String>>())
    }

    @Test
    fun `selecting past the swarm cap is ignored and reports an error`() = runTest {
        repo.entriesFlow.value = (1..17).map { LibraryEntry("$it", "log$it.ulg", 1L, 0L) }
        val vm = ReplayLibraryViewModel(repo)
        vm.onAction(ReplayLibraryAction.OnToggleSelectionMode)

        vm.events.test {
            (1..16).forEach { vm.onAction(ReplayLibraryAction.OnEntryClicked("$it")) }
            expectNoEvents()

            vm.onAction(ReplayLibraryAction.OnEntryClicked("17"))
            assertThat(awaitItem()).isInstanceOf(ReplayLibraryEvent.ShowError::class)
        }
        assertThat(vm.state.value.selectedIds).isEqualTo((1..16).map { "$it" })
    }

    @Test
    fun `play together stages the selection in order and launches with display names`() = runTest {
        repo.entriesFlow.value = listOf(
            LibraryEntry("1", "alpha.ulg", 1L, 0L),
            LibraryEntry("2", "bravo.ulg", 1L, 0L),
            LibraryEntry("3", "charlie.ulg", 1L, 0L),
        )
        val vm = ReplayLibraryViewModel(repo)
        vm.onAction(ReplayLibraryAction.OnToggleSelectionMode)
        vm.onAction(ReplayLibraryAction.OnEntryClicked("3"))
        vm.onAction(ReplayLibraryAction.OnEntryClicked("1"))

        vm.events.test {
            vm.onAction(ReplayLibraryAction.OnPlayTogetherClicked)
            assertThat(awaitItem()).isEqualTo(
                ReplayLibraryEvent.LaunchReplay(
                    entryIds = listOf("3", "1"),
                    droneLabels = listOf("charlie.ulg", "alpha.ulg"),
                ),
            )
        }
        assertThat(repo.stagedBatches).containsExactly(listOf("3", "1"))
        assertThat(vm.state.value.isSelectionMode).isFalse()
        assertThat(vm.state.value.selectedIds).isEqualTo(emptyList())
    }

    @Test
    fun `play together stage failure reports an error and keeps the selection`() = runTest {
        repo.entriesFlow.value = listOf(
            LibraryEntry("1", "a.ulg", 1L, 0L),
            LibraryEntry("2", "b.ulg", 1L, 0L),
        )
        repo.stageResult = Result.Error(DataError.Local.NOT_FOUND)
        val vm = ReplayLibraryViewModel(repo)
        vm.onAction(ReplayLibraryAction.OnToggleSelectionMode)
        vm.onAction(ReplayLibraryAction.OnEntryClicked("1"))
        vm.onAction(ReplayLibraryAction.OnEntryClicked("2"))

        vm.events.test {
            vm.onAction(ReplayLibraryAction.OnPlayTogetherClicked)
            assertThat(awaitItem()).isInstanceOf(ReplayLibraryEvent.ShowError::class)
        }
        assertThat(vm.state.value.isSelectionMode).isTrue()
        assertThat(vm.state.value.selectedIds).containsExactly("1", "2")
    }

    @Test
    fun `play together with fewer than two selections does nothing`() = runTest {
        repo.entriesFlow.value = listOf(LibraryEntry("1", "a.ulg", 1L, 0L))
        val vm = ReplayLibraryViewModel(repo)
        vm.onAction(ReplayLibraryAction.OnToggleSelectionMode)
        vm.onAction(ReplayLibraryAction.OnEntryClicked("1"))

        vm.onAction(ReplayLibraryAction.OnPlayTogetherClicked)

        assertThat(repo.stagedBatches).isEqualTo(emptyList<List<String>>())
        assertThat(vm.state.value.isSelectionMode).isTrue()
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
