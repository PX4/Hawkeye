package com.px4.hawkeye.core.presentation

import com.px4.hawkeye.core.domain.DataError

fun DataError.toUiText(): UiText = when (this) {
    DataError.Local.DISK_FULL -> UiText.StringResource(R.string.core_presentation_error_disk_full)
    DataError.Local.NOT_FOUND -> UiText.StringResource(R.string.core_presentation_error_not_found)
    DataError.Local.UNKNOWN -> UiText.StringResource(R.string.core_presentation_error_unknown)
}
