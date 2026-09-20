package com.px4.hawkeye.feature.live.data

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

/**
 * Locks the API level at which local network access starts being policed.
 *
 * This is the one rule here that a device cannot check: every device we can run against is
 * API 37, so only the enforced branch is reachable on hardware. Get it wrong in the other
 * direction and Live breaks permanently on the whole Android 10 to 16 install base, because
 * the platform there does not define the permission and can never grant it.
 */
class AndroidLocalNetworkPermissionTest {

    @Test
    fun `not enforced below api 37, where the permission does not exist`() {
        assertThat(AndroidLocalNetworkPermission.isEnforced(29)).isFalse() // minSdk
        assertThat(AndroidLocalNetworkPermission.isEnforced(33)).isFalse()
        assertThat(AndroidLocalNetworkPermission.isEnforced(36)).isFalse() // Android 16
    }

    @Test
    fun `enforced from api 37 up`() {
        assertThat(AndroidLocalNetworkPermission.isEnforced(37)).isTrue() // Android 17
        assertThat(AndroidLocalNetworkPermission.isEnforced(38)).isTrue()
    }
}
