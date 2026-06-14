package com.px4.hawkeye.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.feature.replay.presentation.LibraryEntryUi
import com.px4.hawkeye.feature.replay.presentation.ReplayLibraryAction
import com.px4.hawkeye.feature.replay.presentation.ReplayLibraryScreen
import com.px4.hawkeye.feature.replay.presentation.ReplayLibraryState
import com.px4.hawkeye.feature.replay.presentation.ReplayLibraryTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Drives [ReplayLibraryScreen] directly with injected state and a capturing `onAction`, so the
 * new selection flows (tap-and-hold to select, the selection-bar delete action, and the
 * batch-delete confirmation) are verified without a device DB or the renderer.
 */
class ReplayLibrarySelectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val robot by lazy { ReplayLibraryRobot(composeRule) }

    @Test
    fun longPressingARow_emitsOnEntryLongClicked() {
        robot
            .setContent(browseState())
            .assertRowVisible(FIRST)
            .longPressRow(FIRST)
            .assertEmitted(ReplayLibraryAction.OnEntryLongClicked("1"))
    }

    @Test
    fun selectionMode_showsDeleteAction_andEmitsClick() {
        robot
            .setContent(selectionState(selectedIds = listOf("1")))
            .clickDeleteAction()
            .assertEmitted(ReplayLibraryAction.OnDeleteSelectedClicked)
    }

    @Test
    fun deleteAction_isDisabled_whenNothingSelected() {
        robot
            .setContent(selectionState(selectedIds = emptyList()))
            .assertDeleteActionDisabled()
    }

    @Test
    fun confirmationDialog_confirm_emitsOnConfirmDelete() {
        robot
            .setContent(selectionState(selectedIds = listOf("1"), showDeleteDialog = true))
            .assertTitleDisplayed("Delete 1 log?")
            .confirmDelete()
            .assertEmitted(ReplayLibraryAction.OnConfirmDelete)
    }

    @Test
    fun confirmationDialog_cancel_emitsOnDismissDelete() {
        robot
            .setContent(selectionState(selectedIds = listOf("1"), showDeleteDialog = true))
            .cancelDelete()
            .assertEmitted(ReplayLibraryAction.OnDismissDelete)
    }

    @Test
    fun confirmationDialog_usesPluralTitle_forMultipleSelection() {
        robot
            .setContent(selectionState(selectedIds = listOf("1", "2"), showDeleteDialog = true))
            .assertTitleDisplayed("Delete 2 logs?")
    }

    // The AlertDialog owns its own back handling, so system back while it is open must dismiss
    // the dialog (OnDismissDelete) rather than fall through to the screen's selection-mode
    // BackHandler (OnToggleSelectionMode) and wipe the selection.
    @Test
    fun systemBack_whileDialogOpen_dismissesDialog_notSelectionMode() {
        robot
            .setContent(selectionState(selectedIds = listOf("1"), showDeleteDialog = true))
            .pressBack()
            .assertEmitted(ReplayLibraryAction.OnDismissDelete)
    }

    private fun browseState() = ReplayLibraryState(
        isLoading = false,
        entries = ENTRIES,
    )

    private fun selectionState(
        selectedIds: List<String>,
        showDeleteDialog: Boolean = false,
    ) = ReplayLibraryState(
        isLoading = false,
        entries = ENTRIES,
        isSelectionMode = true,
        selectedIds = selectedIds,
        showDeleteDialog = showDeleteDialog,
    )

    private companion object {
        const val FIRST = "flight_2026_05_28.ulg"
        const val SECOND = "sitl_test.ulg"
        val ENTRIES = listOf(
            LibraryEntryUi("1", FIRST, "12.4 MB", "May 28, 2026"),
            LibraryEntryUi("2", SECOND, "3.1 MB", "May 27, 2026"),
        )
    }
}

/** Encapsulates the Compose interactions for [ReplayLibraryScreen] (see the testing skill). */
private class ReplayLibraryRobot(private val composeRule: ComposeContentTestRule) {

    private val actions = mutableListOf<ReplayLibraryAction>()

    fun setContent(state: ReplayLibraryState) = apply {
        actions.clear()
        composeRule.setContent {
            HawkeyeTheme {
                ReplayLibraryScreen(
                    state = state,
                    onAction = { actions += it },
                    onBack = {},
                )
            }
        }
    }

    fun longPressRow(name: String) = apply {
        composeRule.onNodeWithText(name).performTouchInput { longClick() }
    }

    fun clickDeleteAction() = apply {
        composeRule.onNodeWithTag(ReplayLibraryTestTags.DELETE_ACTION).performClick()
    }

    fun confirmDelete() = apply {
        composeRule.onNodeWithTag(ReplayLibraryTestTags.DELETE_DIALOG_CONFIRM).performClick()
    }

    fun cancelDelete() = apply {
        composeRule.onNodeWithTag(ReplayLibraryTestTags.DELETE_DIALOG_CANCEL).performClick()
    }

    fun pressBack() = apply {
        composeRule.waitForIdle()
        Espresso.pressBack()
        composeRule.waitForIdle()
    }

    fun assertRowVisible(name: String) = apply {
        composeRule.onNodeWithText(name).assertIsDisplayed()
    }

    fun assertDeleteActionDisabled() = apply {
        composeRule.onNodeWithTag(ReplayLibraryTestTags.DELETE_ACTION).assertIsNotEnabled()
    }

    fun assertTitleDisplayed(title: String) = apply {
        composeRule.onNodeWithText(title).assertIsDisplayed()
    }

    // Strict: a single gesture must produce exactly one action (e.g. a long press must not
    // also fire OnEntryClicked), so assert the whole captured list, not just membership.
    fun assertEmitted(action: ReplayLibraryAction) = apply {
        assertEquals(listOf(action), actions)
    }
}
