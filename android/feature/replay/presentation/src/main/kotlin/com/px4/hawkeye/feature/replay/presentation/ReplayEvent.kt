package com.px4.hawkeye.feature.replay.presentation

import com.px4.hawkeye.core.presentation.UiText

sealed interface ReplayEvent {
    data class ShowToast(val text: UiText) : ReplayEvent
}
