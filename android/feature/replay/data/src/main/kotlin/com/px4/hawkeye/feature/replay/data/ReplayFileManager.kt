package com.px4.hawkeye.feature.replay.data

import com.px4.hawkeye.core.domain.DataError
import com.px4.hawkeye.core.domain.EmptyResult
import com.px4.hawkeye.core.domain.Result

/**
 * Filesystem side of the library that the repository depends on. Takes the document [uri]
 * as a String (the Android `Uri` parsing lives in [AndroidReplayFileManager]) so the
 * repository stays platform-free and unit-testable with a fake.
 */
interface ReplayFileManager {
    /** Streams the document at [uri] into the library under [fileName]; returns bytes. */
    fun import(uri: String, fileName: String): Result<Long, DataError.Local>

    /**
     * Copies the library payloads [fileNames] (order = drone order) into the renderer inbox
     * and bumps the sentinel.
     */
    fun stage(fileNames: List<String>): EmptyResult<DataError.Local>

    /** Removes the library payload [fileName]. */
    fun delete(fileName: String)

    /** Human-readable name for [uri] — the provider's DISPLAY_NAME, else the last path segment. */
    fun resolveDisplayName(uri: String): String
}
