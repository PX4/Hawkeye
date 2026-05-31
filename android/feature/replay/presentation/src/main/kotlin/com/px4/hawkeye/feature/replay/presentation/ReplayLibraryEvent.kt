package com.px4.hawkeye.feature.replay.presentation

import com.px4.hawkeye.core.presentation.UiText

sealed interface ReplayLibraryEvent {
    /** Open the system document picker. */
    data object LaunchFilePicker : ReplayLibraryEvent

    /** A log was staged into the inbox; hand off to the renderer. */
    data class LaunchReplay(val entryId: String) : ReplayLibraryEvent

    data class ShowError(val text: UiText) : ReplayLibraryEvent
}
