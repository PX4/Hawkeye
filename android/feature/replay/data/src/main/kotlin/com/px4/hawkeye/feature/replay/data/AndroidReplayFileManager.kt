package com.px4.hawkeye.feature.replay.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.px4.hawkeye.core.domain.DataError
import com.px4.hawkeye.core.domain.EmptyResult
import com.px4.hawkeye.core.domain.Result

/**
 * SAF-backed [ReplayFileManager]: resolves a picked document's name and streams its bytes
 * into the library via [LibraryFileStore]. The file-system work itself lives in the store
 * so it stays unit-testable; this class is the thin Android-content glue.
 */
class AndroidReplayFileManager(
    private val contentResolver: ContentResolver,
    private val store: LibraryFileStore,
) : ReplayFileManager {

    override fun import(uri: String, fileName: String): Result<Long, DataError.Local> {
        val input = runCatching { contentResolver.openInputStream(Uri.parse(uri)) }.getOrNull()
            ?: return Result.Error(DataError.Local.NOT_FOUND)
        return store.write(input, fileName)
    }

    override fun stage(fileName: String): EmptyResult<DataError.Local> = store.stage(fileName)

    override fun delete(fileName: String) = store.delete(fileName)

    override fun resolveDisplayName(uri: String): String {
        val parsed = Uri.parse(uri)
        if (parsed.scheme == ContentResolver.SCHEME_CONTENT) {
            runCatching {
                contentResolver.query(
                    parsed,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) cursor.getString(index)?.let { return it }
                    }
                }
            }
        }
        return parsed.lastPathSegment ?: uri
    }
}
