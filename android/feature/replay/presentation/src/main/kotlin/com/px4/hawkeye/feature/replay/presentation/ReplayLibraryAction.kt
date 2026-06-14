package com.px4.hawkeye.feature.replay.presentation

sealed interface ReplayLibraryAction {
    data object OnOpenFileClicked : ReplayLibraryAction
    data class OnFilePicked(val uri: String?) : ReplayLibraryAction
    data class OnEntryClicked(val id: String) : ReplayLibraryAction

    /** Tap-and-hold a row: starts selection mode on that row, or toggles it if already selecting. */
    data class OnEntryLongClicked(val id: String) : ReplayLibraryAction

    /** Trash action in the selection top bar: opens the batch-delete confirmation. */
    data object OnDeleteSelectedClicked : ReplayLibraryAction
    data object OnConfirmDelete : ReplayLibraryAction
    data object OnDismissDelete : ReplayLibraryAction

    /** Enter/exit multi-select; exiting clears the current selection. */
    data object OnToggleSelectionMode : ReplayLibraryAction

    /** Stage the selected logs as one swarm session and launch the renderer. */
    data object OnPlayTogetherClicked : ReplayLibraryAction
}
