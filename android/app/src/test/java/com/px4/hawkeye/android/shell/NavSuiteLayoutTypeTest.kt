package com.px4.hawkeye.android.shell

import android.content.res.Configuration
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class NavSuiteLayoutTypeTest {

    @Test
    fun `landscape uses a navigation rail`() {
        assertThat(navSuiteLayoutType(Configuration.ORIENTATION_LANDSCAPE))
            .isEqualTo(NavigationSuiteType.NavigationRail)
    }

    @Test
    fun `portrait uses a bottom navigation bar`() {
        assertThat(navSuiteLayoutType(Configuration.ORIENTATION_PORTRAIT))
            .isEqualTo(NavigationSuiteType.NavigationBar)
    }

    @Test
    fun `undefined orientation falls back to the bottom bar`() {
        assertThat(navSuiteLayoutType(Configuration.ORIENTATION_UNDEFINED))
            .isEqualTo(NavigationSuiteType.NavigationBar)
    }
}
