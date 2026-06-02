package com.px4.hawkeye.core.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class LivePortTest {

    private fun successOf(raw: String): Int? =
        (validateListenPort(raw) as? Result.Success)?.data

    private fun errorOf(raw: String): Error? =
        (validateListenPort(raw) as? Result.Error)?.error

    @Test fun `valid mid-range port succeeds`() {
        assertThat(successOf("14550")).isEqualTo(14550)
    }

    @Test fun `default port succeeds`() {
        assertThat(successOf(DEFAULT_LIVE_PORT.toString())).isEqualTo(DEFAULT_LIVE_PORT)
    }

    @Test fun `lower boundary 1024 succeeds and 1023 fails`() {
        assertThat(successOf("1024")).isEqualTo(1024)
        assertThat(errorOf("1023")).isEqualTo(PortValidationError.OUT_OF_RANGE)
    }

    @Test fun `upper boundary 65535 succeeds and 65536 fails`() {
        assertThat(successOf("65535")).isEqualTo(65535)
        assertThat(errorOf("65536")).isEqualTo(PortValidationError.OUT_OF_RANGE)
    }

    @Test fun `surrounding whitespace is trimmed`() {
        assertThat(successOf("  19410  ")).isEqualTo(19410)
    }

    @Test fun `non-numeric input is rejected`() {
        assertThat(errorOf("abc")).isEqualTo(PortValidationError.NOT_A_NUMBER)
    }

    @Test fun `empty input is rejected`() {
        assertThat(errorOf("")).isEqualTo(PortValidationError.NOT_A_NUMBER)
    }

    @Test fun `negative port is out of range`() {
        assertThat(errorOf("-1")).isEqualTo(PortValidationError.OUT_OF_RANGE)
    }
}
