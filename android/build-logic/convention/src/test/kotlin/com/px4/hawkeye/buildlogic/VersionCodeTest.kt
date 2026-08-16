package com.px4.hawkeye.buildlogic

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.messageContains
import org.junit.jupiter.api.Test

class VersionCodeTest {

    @Test
    fun `final releases take rc component 99`() {
        assertThat(hawkeyeVersionCode("0.4.0")).isEqualTo(400_099)
        assertThat(hawkeyeVersionCode("1.2.3")).isEqualTo(100_200_399)
        // The old scheme shipped v0.3.0 as 3000; every new code must stay above it.
        assertThat(hawkeyeVersionCode("0.3.0")).isEqualTo(300_099)
        // Largest representable version stays under Google Play's 2,100,000,000 cap.
        assertThat(hawkeyeVersionCode("20.999.999")).isEqualTo(2_099_999_999)
    }

    @Test
    fun `rc suffixes sort below their final release and above the previous one`() {
        assertThat(hawkeyeVersionCode("0.4.0-rc1")).isEqualTo(400_001)
        assertThat(hawkeyeVersionCode("0.4.0-rc98")).isEqualTo(400_098)
        assertThat(hawkeyeVersionCode("0.4.1-rc1")).isEqualTo(400_101)
    }

    @Test
    fun `dev and ci builds floor to 1`() {
        assertThat(hawkeyeVersionCode("0.0.0-dev")).isEqualTo(1)
        assertThat(hawkeyeVersionCode("0.0.0-ci")).isEqualTo(1)
        // A non-zero version with the suffix keeps its computed code.
        assertThat(hawkeyeVersionCode("0.4.0-dev")).isEqualTo(400_000)
    }

    @Test
    fun `trailing hyphen is rejected instead of taking the final release's code`() {
        assertFailure { hawkeyeVersionCode("0.4.0-") }
            .messageContains("'0.4.0-'")
    }

    @Test
    fun `malformed rc suffixes are rejected`() {
        assertFailure { hawkeyeVersionCode("0.4.0-rc0") }.messageContains("rc1..rc98")
        assertFailure { hawkeyeVersionCode("0.4.0-rc01") }.messageContains("rc1..rc98")
        assertFailure { hawkeyeVersionCode("0.4.0-rc100") }.messageContains("rc1..rc98")
        assertFailure { hawkeyeVersionCode("0.4.0-RC1") }.messageContains("rc1..rc98")
        assertFailure { hawkeyeVersionCode("0.4.0-rc1-hotfix") }.messageContains("rc1..rc98")
    }

    @Test
    fun `rc99 is rejected because it collides with the final release`() {
        assertFailure { hawkeyeVersionCode("0.4.0-rc99") }
            .messageContains("rc99 collides")
    }

    @Test
    fun `unknown suffixes are rejected`() {
        assertFailure { hawkeyeVersionCode("0.4.0-beta1") }.messageContains("beta1")
        assertFailure { hawkeyeVersionCode("0.4.0-snapshot") }.messageContains("snapshot")
    }

    @Test
    fun `major above 20 is rejected to stay under the Play cap`() {
        assertFailure { hawkeyeVersionCode("21.0.0") }.messageContains("cap")
    }

    @Test
    fun `malformed base versions are rejected`() {
        assertFailure { hawkeyeVersionCode("0.4") }.messageContains("MAJOR.MINOR.PATCH")
        assertFailure { hawkeyeVersionCode("0.4.0.1") }.messageContains("MAJOR.MINOR.PATCH")
        assertFailure { hawkeyeVersionCode("a.b.c") }.messageContains("0..999")
        assertFailure { hawkeyeVersionCode("0.1000.0") }.messageContains("0..999")
        // The hyphen splits before the base parse, so this fails the shape check.
        assertFailure { hawkeyeVersionCode("0.-1.0") }.messageContains("MAJOR.MINOR.PATCH")
        assertFailure { hawkeyeVersionCode("") }.messageContains("MAJOR.MINOR.PATCH")
    }
}
