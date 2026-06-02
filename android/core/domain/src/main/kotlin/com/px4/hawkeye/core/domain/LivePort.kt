package com.px4.hawkeye.core.domain

/** Default MAVLink listen port, matching the desktop SITL stream (px4-rc.mavlink). */
const val DEFAULT_LIVE_PORT: Int = 19410

/** Valid UDP listen ports: avoid the privileged range (<1024) and stay within 16 bits. */
val LIVE_PORT_RANGE: IntRange = 1024..65535

enum class PortValidationError : Error { NOT_A_NUMBER, OUT_OF_RANGE }

/** Parses and validates user-entered port text against [LIVE_PORT_RANGE]. */
fun validateListenPort(raw: String): Result<Int, PortValidationError> {
    val port = raw.trim().toIntOrNull()
        ?: return Result.Error(PortValidationError.NOT_A_NUMBER)
    if (port !in LIVE_PORT_RANGE) return Result.Error(PortValidationError.OUT_OF_RANGE)
    return Result.Success(port)
}
