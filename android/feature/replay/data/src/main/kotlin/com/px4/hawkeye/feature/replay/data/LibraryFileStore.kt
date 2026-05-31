package com.px4.hawkeye.feature.replay.data

import com.px4.hawkeye.core.domain.DataError
import com.px4.hawkeye.core.domain.EmptyResult
import com.px4.hawkeye.core.domain.Result
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/**
 * The library's on-disk side: imported payloads live under `filesDir/library/`, and
 * staging copies one into `filesDir/inbox/current.ulg` and bumps the `.ready` sentinel
 * the native poll loop reads (a millis token, so two stages in the same wall-clock
 * second are still distinguishable).
 *
 * Pure file/JVM logic with no Android dependencies, so it is unit-testable against a
 * temp directory. [clock] is injected for deterministic sentinel tokens in tests.
 */
class LibraryFileStore(
    filesDir: File,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val libraryDir = File(filesDir, "library")
    private val inboxDir = File(filesDir, "inbox")

    /** Streams [source] into the library under [fileName] (atomic via .tmp + rename). */
    fun write(source: InputStream, fileName: String): Result<Long, DataError.Local> = runCatching {
        libraryDir.mkdirs()
        val target = File(libraryDir, fileName)
        val tmp = File(libraryDir, "$fileName.tmp")
        val bytes = source.use { input -> tmp.outputStream().use { output -> input.copyTo(output) } }
        if (!tmp.renameTo(target)) {
            tmp.delete()
            throw IOException("renameTo $target failed")
        }
        bytes
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(classify(it)) },
    )

    /** Copies the library payload [fileName] into the inbox and bumps the sentinel. */
    fun stage(fileName: String): EmptyResult<DataError.Local> = runCatching {
        val source = File(libraryDir, fileName)
        if (!source.exists()) throw FileNotFoundException("missing library file $fileName")
        inboxDir.mkdirs()
        val target = File(inboxDir, "current.ulg")
        val tmp = File(inboxDir, "current.ulg.tmp")
        source.inputStream().use { input -> tmp.outputStream().use { output -> input.copyTo(output) } }
        if (!tmp.renameTo(target)) {
            tmp.delete()
            throw IOException("renameTo $target failed")
        }
        File(inboxDir, ".ready").writeText(clock().toString())
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { Result.Error(classify(it)) },
    )

    /** Removes the library payload [fileName]; no-op if it is already gone. */
    fun delete(fileName: String) {
        File(libraryDir, fileName).delete()
    }

    private fun classify(e: Throwable): DataError.Local = when {
        e is FileNotFoundException -> DataError.Local.NOT_FOUND
        e is IOException && e.message?.contains("ENOSPC", ignoreCase = true) == true ->
            DataError.Local.DISK_FULL
        e is IOException && e.message?.contains("space", ignoreCase = true) == true ->
            DataError.Local.DISK_FULL
        else -> DataError.Local.UNKNOWN
    }
}
