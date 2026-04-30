package com.px4.hawkeye.android

// region PROTOTYPE_PROMPT
// Throwaway prototype UI: Material 3 confirm dialog before opening a shared
// .ulg, plus an empty-state notice when the activity launches with no replay
// available. To remove: delete this file, drop the PROMPT_BEFORE_OPEN sites in
// HawkeyeActivity, and drop the prompt_* strings in strings.xml.

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

object IntentPromptDialog {

    // Holds the most recently shown dialog so a subsequent intent can dismiss it
    // before showing a fresh one (rapid re-share = "user wants the latest file").
    private var current: WeakReference<AlertDialog>? = null

    // Tracks the in-flight name-resolution coroutine. Cancelling it before
    // launching a new one guarantees that whichever intent arrives last is the
    // one whose dialog actually appears — without this, two near-simultaneous
    // intents could resolve to Main in IO-completion order, not arrival order.
    private var pendingJob: Job? = null

    /**
     * Shows a Material 3 "Open ULog?" confirmation. Resolves the human-readable
     * filename and source authority off the main thread (ContentResolver.query
     * can block briefly on cold cursors), then renders the dialog on Main.
     * Invokes [onConfirm] on the Main thread when the user taps Open.
     */
    fun confirmOpen(activity: Activity, scope: CoroutineScope, uri: Uri, onConfirm: () -> Unit) {
        pendingJob?.cancel()
        pendingJob = scope.launch {
            val name = withContext(Dispatchers.IO) { resolveDisplayName(activity, uri) }
            val source = uri.authority?.takeIf { it.isNotBlank() } ?: uri.path ?: uri.toString()

            current?.get()?.dismiss()
            val dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.prompt_open_title)
                .setMessage("$name\n${activity.getString(R.string.prompt_from_prefix, source)}")
                .setNegativeButton(R.string.prompt_cancel) { d, _ -> d.dismiss() }
                .setPositiveButton(R.string.prompt_open) { d, _ ->
                    d.dismiss()
                    onConfirm()
                }
                .setCancelable(true)
                .show()
            current = WeakReference(dialog)
        }
    }

    /** Empty-state notice shown when the activity launches with no replay. */
    fun showNoFileLoaded(activity: Activity) {
        current?.get()?.dismiss()
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.prompt_no_file_title)
            .setMessage(R.string.prompt_no_file_message)
            .setPositiveButton(R.string.prompt_ok) { d, _ -> d.dismiss() }
            .setCancelable(true)
            .show()
        current = WeakReference(dialog)
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String {
        if (uri.scheme == "content") {
            runCatching {
                context.contentResolver.query(
                    uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) return cursor.getString(idx) ?: uri.toString()
                    }
                }
            }
        }
        return uri.lastPathSegment ?: uri.toString()
    }
}
// endregion
