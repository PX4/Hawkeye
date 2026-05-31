package com.px4.hawkeye.feature.replay.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for one imported log. [fileName] is the payload's name under
 * `filesDir/library/` — kept on the row so deletion and staging can find the file
 * without re-deriving it from the id.
 */
@Entity(tableName = "library_entries")
data class LibraryEntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "imported_at_millis") val importedAtMillis: Long,
    @ColumnInfo(name = "file_name") val fileName: String,
)
