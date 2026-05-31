package com.px4.hawkeye.feature.replay.data

import com.px4.hawkeye.feature.replay.data.db.LibraryEntryEntity
import com.px4.hawkeye.core.domain.LibraryEntry

internal fun LibraryEntryEntity.toLibraryEntry(): LibraryEntry = LibraryEntry(
    id = id,
    displayName = displayName,
    sizeBytes = sizeBytes,
    importedAtMillis = importedAtMillis,
)

internal fun LibraryEntry.toEntity(fileName: String): LibraryEntryEntity = LibraryEntryEntity(
    id = id,
    displayName = displayName,
    sizeBytes = sizeBytes,
    importedAtMillis = importedAtMillis,
    fileName = fileName,
)
