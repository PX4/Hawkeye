package com.px4.hawkeye.feature.replay.presentation

data class ReplayState(
    val dialog: ReplayDialog? = null,
    val isIngesting: Boolean = false
)

sealed interface ReplayDialog {
    data class ConfirmOpen(
        val displayName: String,
        val source: String
    ) : ReplayDialog

    data object NoFileLoaded : ReplayDialog
}
