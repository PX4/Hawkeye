package com.px4.hawkeye.feature.replay.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.px4.hawkeye.core.domain.onFailure
import com.px4.hawkeye.core.domain.onSuccess
import com.px4.hawkeye.core.presentation.toUiText
import com.px4.hawkeye.core.domain.LibraryEntry
import com.px4.hawkeye.core.domain.ReplayLibraryRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReplayLibraryViewModel(
    private val repository: ReplayLibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReplayLibraryState())
    val state = _state.asStateFlow()

    private val _events = Channel<ReplayLibraryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.observeLibrary().collect { entries ->
                _state.update { it.copy(entries = entries.map(LibraryEntry::toUi), isLoading = false) }
            }
        }
    }

    fun onAction(action: ReplayLibraryAction) {
        when (action) {
            ReplayLibraryAction.OnOpenFileClicked ->
                viewModelScope.launch { _events.send(ReplayLibraryEvent.LaunchFilePicker) }

            is ReplayLibraryAction.OnFilePicked -> action.uri?.let(::importFile)

            is ReplayLibraryAction.OnEntryClicked -> stageAndLaunch(action.id)

            is ReplayLibraryAction.OnDeleteRequested ->
                _state.update { state -> state.copy(pendingDelete = state.entries.find { it.id == action.id }) }

            ReplayLibraryAction.OnConfirmDelete -> confirmDelete()

            ReplayLibraryAction.OnDismissDelete ->
                _state.update { it.copy(pendingDelete = null) }
        }
    }

    private fun importFile(uri: String) {
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true) }
            repository.import(uri).onFailure { _events.send(ReplayLibraryEvent.ShowError(it.toUiText())) }
            _state.update { it.copy(isImporting = false) }
        }
    }

    private fun stageAndLaunch(id: String) {
        viewModelScope.launch {
            repository.stageForPlayback(id)
                .onSuccess { _events.send(ReplayLibraryEvent.LaunchReplay(id)) }
                .onFailure { _events.send(ReplayLibraryEvent.ShowError(it.toUiText())) }
        }
    }

    private fun confirmDelete() {
        val pending = _state.value.pendingDelete ?: return
        _state.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            repository.delete(pending.id).onFailure { _events.send(ReplayLibraryEvent.ShowError(it.toUiText())) }
        }
    }
}
