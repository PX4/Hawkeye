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
 * staging copies them into `filesDir/inbox/` (`current.ulg`, plus `swarm_<i>.ulg` for a
 * multi-drone session) and bumps the `.ready` sentinel the native poll loop reads (a
 * millis token, so two stages in the same wall-clock second are still distinguishable).
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
    fun stage(fileName: String): EmptyResult<DataError.Local> = stage(listOf(fileName))

    /**
     * Stages [fileNames] (staged order = drone order) into the inbox and bumps the sentinel.
     * Index 0 keeps the legacy `current.ulg` name; indices 1..n-1 become `swarm_<i>.ulg`. A
     * single file writes the legacy bare-millis token; several write `"<millis> <count>"` for
     * the native swarm loader (an older binary's strtoll stops at the space and still reads
     * the millis). All sources are validated before the inbox is touched, so a failed batch
     * never clobbers the previous session.
     */
    fun stage(fileNames: List<String>): EmptyResult<DataError.Local> = runCatching {
        if (fileNames.isEmpty()) throw FileNotFoundException("empty stage batch")
        val sources = fileNames.map { name ->
            File(libraryDir, name).also {
                if (!it.exists()) throw FileNotFoundException("missing library file $name")
            }
        }
        inboxDir.mkdirs()
        inboxDir.listFiles { file -> SWARM_FILE_PATTERN.matches(file.name) }
            ?.forEach { it.delete() }
        sources.forEachIndexed { index, source ->
            val targetName = if (index == 0) "current.ulg" else "swarm_$index.ulg"
            val target = File(inboxDir, targetName)
            val tmp = File(inboxDir, "$targetName.tmp")
            source.inputStream().use { input -> tmp.outputStream().use { output -> input.copyTo(output) } }
            if (!tmp.renameTo(target)) {
                tmp.delete()
                throw IOException("renameTo $target failed")
            }
        }
        val token = clock().toString()
        File(inboxDir, ".ready")
            .writeText(if (fileNames.size == 1) token else "$token ${fileNames.size}")
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

    private companion object {
        /** Extra swarm payloads (and their tmp files) from a previous multi-drone session. */
        val SWARM_FILE_PATTERN = Regex("""swarm_\d+\.ulg(\.tmp)?""")
    }
}
