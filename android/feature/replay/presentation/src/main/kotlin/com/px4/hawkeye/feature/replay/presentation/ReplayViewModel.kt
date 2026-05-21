package com.px4.hawkeye.feature.replay.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.px4.hawkeye.core.domain.onFailure
import com.px4.hawkeye.core.domain.onSuccess
import com.px4.hawkeye.core.presentation.UiText
import com.px4.hawkeye.feature.replay.domain.UlogInboxDataSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReplayViewModel(
    private val ulogInbox: UlogInboxDataSource
) : ViewModel() {

    private val _state = MutableStateFlow(ReplayState())
    val state = _state.asStateFlow()

    // Buffered (not rendezvous) so emitting an event never blocks ingest/preview
    // coroutines waiting for a collector — `ObserveAsEvents` stops collecting when
    // the lifecycle drops below STARTED, and a rendezvous `send` would suspend
    // there, leaving e.g. `isIngesting = true` until the user returns to the app.
    private val _events = Channel<ReplayEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var pendingUri: String? = null
    private var previewJob: Job? = null

    fun onAction(action: ReplayAction) {
        when (action) {
            is ReplayAction.OnAppStarted -> handleAppStarted(action.fromFreshIngest)
            is ReplayAction.OnIntentReceived -> handleIntent(action.uri)
            ReplayAction.OnConfirmOpen -> ingestPendingUri()
            ReplayAction.OnDismissDialog -> dismissDialog()
        }
    }

    private fun handleAppStarted(fromFreshIngest: Boolean) {
        if (fromFreshIngest) {
            // The trampoline just wrote a file the user explicitly asked for; leave
            // the inbox alone so the native poll loop picks it up.
            return
        }
        // Plain cold launch (e.g., user tapped the app icon). Don't keep replaying a
        // .ulg from a previous session — wipe the inbox and show the empty-state dialog.
        viewModelScope.launch {
            ulogInbox.clearInbox()
            _state.update { it.copy(dialog = ReplayDialog.NoFileLoaded) }
        }
    }

    private fun handleIntent(uri: String) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            ulogInbox.preview(uri)
                .onSuccess { preview ->
                    pendingUri = uri
                    _state.update {
                        it.copy(
                            dialog = ReplayDialog.ConfirmOpen(
                                displayName = preview.displayName,
                                source = preview.source
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _events.send(ReplayEvent.ShowToast(error.toUiText()))
                }
        }
    }

    private fun ingestPendingUri() {
        val uri = pendingUri ?: return
        _state.update { it.copy(dialog = null) }
        viewModelScope.launch {
            _state.update { it.copy(isIngesting = true) }
            ulogInbox.ingest(uri)
                .onSuccess { file ->
                    _events.send(
                        ReplayEvent.ShowToast(
                            UiText.StringResource(
                                id = R.string.replay_ingest_success,
                                args = arrayOf(file.sizeBytes)
                            )
                        )
                    )
                }
                .onFailure { error ->
                    _events.send(ReplayEvent.ShowToast(error.toUiText()))
                }
            _state.update { it.copy(isIngesting = false) }
            pendingUri = null
        }
    }

    private fun dismissDialog() {
        previewJob?.cancel()
        pendingUri = null
        _state.update { it.copy(dialog = null) }
    }
}
