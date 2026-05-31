package com.px4.hawkeye.feature.replay.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.matches
import com.px4.hawkeye.core.domain.LibraryEntry
import org.junit.jupiter.api.Test

class LibraryEntryUiMapperTest {

    @Test
    fun `formatSize renders bytes, KB, and MB`() {
        assertThat(formatSize(0L)).isEqualTo("0 B")
        assertThat(formatSize(512L)).isEqualTo("512 B")
        assertThat(formatSize(1024L)).isEqualTo("1.0 KB")
        assertThat(formatSize(1536L)).isEqualTo("1.5 KB")
        assertThat(formatSize(5L * 1024 * 1024)).isEqualTo("5.0 MB")
    }

    @Test
    fun `toUi formats size and a localized date`() {
        // 2026-06-15T12:00:00Z — mid-day so any reasonable time zone still lands in 2026.
        val ui = LibraryEntry("1", "flight.ulg", 2L * 1024 * 1024, 1_781_870_400_000L).toUi()

        assertThat(ui.sizeLabel).isEqualTo("2.0 MB")
        assertThat(ui.importedLabel).matches(Regex("""[A-Z][a-z]{2} \d{1,2}, \d{4}"""))
    }
}
