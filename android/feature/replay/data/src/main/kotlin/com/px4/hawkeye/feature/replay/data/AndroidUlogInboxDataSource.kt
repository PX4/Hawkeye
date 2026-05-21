package com.px4.hawkeye.feature.replay.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.px4.hawkeye.core.domain.EmptyResult
import com.px4.hawkeye.core.domain.Result
import com.px4.hawkeye.feature.replay.domain.ReplayError
import com.px4.hawkeye.feature.replay.domain.UlogFile
import com.px4.hawkeye.feature.replay.domain.UlogInboxDataSource
import com.px4.hawkeye.feature.replay.domain.UlogPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Writes inbound .ulg payloads into `filesDir/inbox/current.ulg` and bumps the
 * `.ready` sentinel so the native poll loop picks them up. The sentinel stores a
 * monotonic-millis token (read by the C side as content, not stat-mtime) so two
 * intents in the same wall-clock second are still distinguishable.
 */
class AndroidUlogInboxDataSource(
    private val contentResolver: ContentResolver,
    filesDir: File
) : UlogInboxDataSource {

    private val inbox: File = File(filesDir, "inbox")
    private val target: File = File(inbox, "current.ulg")
    private val tmp: File = File(inbox, "current.ulg.tmp")
    private val ready: File = File(inbox, ".ready")

    override suspend fun preview(uri: String): Result<UlogPreview, ReplayError> =
        withContext(Dispatchers.IO) {
            val parsed = Uri.parse(uri)
            val displayName = resolveDisplayNameSync(parsed)
            val source = parsed.authority?.takeIf { it.isNotBlank() } ?: parsed.path ?: uri
            Result.Success(UlogPreview(displayName = displayName, source = source))
        }

    override suspend fun ingest(uri: String): Result<UlogFile, ReplayError> =
        withContext(Dispatchers.IO) {
            runCatching {
                inbox.mkdirs()
                val parsed = Uri.parse(uri)
                val bytes = (contentResolver.openInputStream(parsed)
                    ?: throw IOException("openInputStream returned null for $uri")).use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output) }
                }
                if (!tmp.renameTo(target)) {
                    tmp.delete()
                    throw IOException("renameTo $target failed")
                }
                ready.writeText(System.currentTimeMillis().toString())

                val displayName = resolveDisplayNameSync(parsed)
                Log.i(TAG, "ingested $uri ($bytes bytes)")
                UlogFile(displayName = displayName, sizeBytes = bytes)
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { e ->
                    Log.e(TAG, "ingest failed for $uri", e)
                    Result.Error(classifyIngestFailure(e))
                }
            )
        }

    override suspend fun clearInbox(): EmptyResult<ReplayError> =
        withContext(Dispatchers.IO) {
            runCatching {
                ready.delete()
                target.delete()
                tmp.delete()
            }.fold(
                onSuccess = { Result.Success(Unit) },
                onFailure = { Result.Error(ReplayError.UNKNOWN) }
            )
        }

    private fun resolveDisplayNameSync(parsed: Uri): String {
        if (parsed.scheme == "content") {
            runCatching {
                contentResolver.query(
                    parsed,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) return cursor.getString(idx) ?: parsed.toString()
                    }
                }
            }
        }
        return parsed.lastPathSegment ?: parsed.toString()
    }

    private fun classifyIngestFailure(e: Throwable): ReplayError = when (e) {
        is IOException -> if (e.message?.contains("openInputStream") == true) {
            ReplayError.OPEN_FAILED
        } else {
            ReplayError.WRITE_FAILED
        }
        else -> ReplayError.UNKNOWN
    }

    private companion object {
        private const val TAG = "Hawkeye"
    }
}
