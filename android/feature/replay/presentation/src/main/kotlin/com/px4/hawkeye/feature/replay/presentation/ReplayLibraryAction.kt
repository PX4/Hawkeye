package com.px4.hawkeye.feature.replay.presentation

sealed interface ReplayLibraryAction {
    data object OnOpenFileClicked : ReplayLibraryAction
    data class OnFilePicked(val uri: String?) : ReplayLibraryAction
    data class OnEntryClicked(val id: String) : ReplayLibraryAction
    data class OnDeleteRequested(val id: String) : ReplayLibraryAction
    data object OnConfirmDelete : ReplayLibraryAction
    data object OnDismissDelete : ReplayLibraryAction
}
