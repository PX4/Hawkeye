package com.px4.hawkeye.feature.replay.presentation

import com.px4.hawkeye.core.presentation.UiText
import com.px4.hawkeye.feature.replay.domain.ReplayError

fun ReplayError.toUiText(): UiText = when (this) {
    ReplayError.OPEN_FAILED -> UiText.StringResource(R.string.replay_error_open_failed)
    ReplayError.WRITE_FAILED -> UiText.StringResource(R.string.replay_error_write_failed)
    ReplayError.UNKNOWN -> UiText.StringResource(R.string.replay_error_unknown)
}
