package com.px4.hawkeye.android.render

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class RenderSessionMarkerTest {

    @Test
    fun `writes the live token and port to inbox dot live`(@TempDir filesDir: File) {
        val inbox = File(filesDir, "inbox")

        writeLiveSessionMarker(inbox, tokenMillis = 1780000000123L, listenPort = 14550)

        val marker = File(inbox, ".live")
        assertThat(marker.exists()).isTrue()
        assertThat(marker.readText()).isEqualTo("1780000000123 14550")
    }

    @Test
    fun `creates the inbox directory if missing`(@TempDir filesDir: File) {
        val inbox = File(filesDir, "inbox")

        writeLiveSessionMarker(inbox, tokenMillis = 42L, listenPort = 19410)

        assertThat(inbox.isDirectory).isTrue()
    }
}
