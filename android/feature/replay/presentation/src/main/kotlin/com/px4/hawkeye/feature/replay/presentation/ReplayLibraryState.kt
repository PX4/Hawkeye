package com.px4.hawkeye.feature.replay.presentation

import androidx.compose.runtime.Stable

@Stable
data class ReplayLibraryState(
    val entries: List<LibraryEntryUi> = emptyList(),
    val isLoading: Boolean = true,
    val isImporting: Boolean = false,
    val pendingDelete: LibraryEntryUi? = null,
)

/** Presentation view of a library entry, with size/date already formatted for display. */
data class LibraryEntryUi(
    val id: String,
    val displayName: String,
    val sizeLabel: String,
    val importedLabel: String,
)
