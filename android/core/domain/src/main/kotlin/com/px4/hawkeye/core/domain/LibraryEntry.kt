package com.px4.hawkeye.core.domain

/**
 * A `.ulg` log imported into the in-app library. The payload bytes live on disk under
 * `filesDir/library/`; this is the metadata the Replay library and the Home recents peek
 * show, and what the repository uses to stage a file for playback.
 *
 * Lives in `core:domain` because more than one feature consumes it (Replay and Home).
 * Parsed flight duration is intentionally absent: extracting it needs the native ULog
 * parser/JNI, which arrives with the transport overlay in a later plan.
 */
data class LibraryEntry(
    val id: String,
    val displayName: String,
    val sizeBytes: Long,
    val importedAtMillis: Long,
)
