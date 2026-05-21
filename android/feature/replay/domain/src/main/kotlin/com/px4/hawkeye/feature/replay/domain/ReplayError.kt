package com.px4.hawkeye.feature.replay.domain

import com.px4.hawkeye.core.domain.Error

enum class ReplayError : Error {
    OPEN_FAILED,
    WRITE_FAILED,
    UNKNOWN
}
