package com.px4.hawkeye.android

import android.app.NativeActivity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * NativeActivity subclass that ingests inbound .ulg files from VIEW/SEND intents.
 *
 * Intents push URIs into a SharedFlow; a Main-bound collector hands each URI to
 * Dispatchers.IO for the actual copy, then surfaces a Toast on completion. The
 * payload is written to inbox/current.ulg.tmp and atomically renamed into place
 * before the inbox/.ready sentinel is updated, so the native poll loop never
 * sees a half-written file. The sentinel stores a monotonic millis token (read
 * by the C side as content, not stat-mtime) so two intents in the same wall
 * clock second are still distinguishable.
 */
class HawkeyeActivity : NativeActivity() {

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val inbound = MutableSharedFlow<Uri>(extraBufferCapacity = 8)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Single collector serializes intents — concurrent .ulg shares can never
        // race the same destination file.
        activityScope.launch {
            inbound.collect { uri ->
                val result = withContext(Dispatchers.IO) { ingest(uri) }
                val msg = result.fold(
                    onSuccess = { bytes -> "Loaded ULog ($bytes bytes)" },
                    onFailure = { e -> "Failed to load ULog: ${e.message ?: e::class.java.simpleName}" }
                )
                Toast.makeText(this@HawkeyeActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }

        val incoming = extractUri(intent)
        // region PROTOTYPE_PROMPT
        // Cold launch with no inbound share = fresh slate. Drop any previous
        // replay payload so the native poll loop starts at origin, then prompt.
        // Backgrounded -> foregrounded does NOT re-enter onCreate, so an
        // in-memory replay survives a Home press unaffected — only a real
        // process restart (force-stop, system kill, days later) hits this.
        if (PROMPT_BEFORE_OPEN && incoming == null) {
            clearInbox()
            IntentPromptDialog.showNoFileLoaded(this)
        }
        // endregion

        offerUri(incoming)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        offerUri(extractUri(intent))
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    private fun offerUri(uri: Uri?) {
        if (uri == null) return
        // region PROTOTYPE_PROMPT
        if (PROMPT_BEFORE_OPEN) {
            IntentPromptDialog.confirmOpen(this, activityScope, uri) { emitInbound(uri) }
            return
        }
        // endregion
        emitInbound(uri)
    }

    private fun emitInbound(uri: Uri) {
        if (!inbound.tryEmit(uri)) {
            Log.w(TAG, "dropped intent (buffer full): $uri")
        }
    }

    // region PROTOTYPE_PROMPT
    private fun clearInbox() {
        val inbox = File(filesDir, "inbox")
        File(inbox, ".ready").delete()
        File(inbox, "current.ulg").delete()
        File(inbox, "current.ulg.tmp").delete()
    }
    // endregion

    private fun extractUri(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> getStreamExtra(intent)
            else -> null
        }
    }

    private fun getStreamExtra(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

    /**
     * Runs on Dispatchers.IO. Writes via .tmp + atomic rename so the canonical
     * path is never observed mid-copy. The sentinel is written last with a
     * fresh millis token; the C side reads the content (not mtime) so f2fs's
     * 1-second mtime granularity can't coalesce back-to-back shares.
     */
    private fun ingest(uri: Uri): Result<Long> = runCatching {
        val inbox = File(filesDir, "inbox").apply { mkdirs() }
        val tmp = File(inbox, "current.ulg.tmp")
        val target = File(inbox, "current.ulg")
        val ready = File(inbox, ".ready")

        val bytes = (contentResolver.openInputStream(uri)
            ?: throw IOException("openInputStream returned null for $uri")).use { input ->
            tmp.outputStream().use { output -> input.copyTo(output) }
        }

        if (!tmp.renameTo(target)) {
            tmp.delete()
            throw IOException("renameTo $target failed")
        }

        ready.writeText(System.currentTimeMillis().toString())
        Log.i(TAG, "ingested $uri ($bytes bytes)")
        bytes
    }.onFailure { Log.e(TAG, "ingest failed for $uri", it) }

    companion object {
        private const val TAG = "Hawkeye"

        // region PROTOTYPE_PROMPT
        // Set to false to bypass the confirm/empty-state dialogs and let intents
        // flow straight through to ingestion, like before this prototype tweak.
        private const val PROMPT_BEFORE_OPEN = true
        // endregion
    }
}