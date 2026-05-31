package com.px4.hawkeye.feature.replay.presentation

import com.px4.hawkeye.core.domain.LibraryEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun LibraryEntry.toUi(): LibraryEntryUi = LibraryEntryUi(
    id = id,
    displayName = displayName,
    sizeLabel = formatSize(sizeBytes),
    importedLabel = formatImported(importedAtMillis),
)

private const val UNIT_STEP = 1024.0

/** Human-readable byte size, e.g. "12.4 MB". Locale.US for a stable, technical label. */
internal fun formatSize(bytes: Long): String {
    val kb = UNIT_STEP
    val mb = kb * UNIT_STEP
    val gb = mb * UNIT_STEP
    return when {
        bytes >= gb -> String.format(Locale.US, "%.1f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.1f KB", bytes / kb)
        else -> "$bytes B"
    }
}

private val importedFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US).withZone(ZoneId.systemDefault())

internal fun formatImported(millis: Long): String =
    importedFormatter.format(Instant.ofEpochMilli(millis))
