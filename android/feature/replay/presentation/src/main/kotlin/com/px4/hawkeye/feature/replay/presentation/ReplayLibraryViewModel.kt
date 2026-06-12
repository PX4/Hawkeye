package com.px4.hawkeye.feature.replay.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.px4.hawkeye.core.domain.onFailure
import com.px4.hawkeye.core.domain.onSuccess
import com.px4.hawkeye.core.presentation.UiText
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

            is ReplayLibraryAction.OnEntryClicked ->
                if (_state.value.isSelectionMode) toggleSelection(action.id)
                else stageAndLaunch(listOf(action.id))

            is ReplayLibraryAction.OnDeleteRequested ->
                _state.update { state -> state.copy(pendingDelete = state.entries.find { it.id == action.id }) }

            ReplayLibraryAction.OnConfirmDelete -> confirmDelete()

            ReplayLibraryAction.OnDismissDelete ->
                _state.update { it.copy(pendingDelete = null) }

            ReplayLibraryAction.OnToggleSelectionMode ->
                _state.update {
                    it.copy(isSelectionMode = !it.isSelectionMode, selectedIds = emptyList())
                }

            ReplayLibraryAction.OnPlayTogetherClicked -> playTogether()
        }
    }

    private fun toggleSelection(id: String) {
        val selected = _state.value.selectedIds
        when {
            id in selected ->
                _state.update { it.copy(selectedIds = it.selectedIds - id) }

            selected.size >= MAX_SWARM ->
                viewModelScope.launch {
                    _events.send(
                        ReplayLibraryEvent.ShowError(
                            UiText.StringResource(R.string.replay_swarm_cap),
                        ),
                    )
                }

            else -> _state.update { it.copy(selectedIds = it.selectedIds + id) }
        }
    }

    private fun playTogether() {
        val ids = _state.value.selectedIds
        if (ids.size < 2) return
        stageAndLaunch(ids)
    }

    private fun importFile(uri: String) {
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true) }
            repository.import(uri).onFailure { _events.send(ReplayLibraryEvent.ShowError(it.toUiText())) }
            _state.update { it.copy(isImporting = false) }
        }
    }

    private fun stageAndLaunch(ids: List<String>) {
        viewModelScope.launch {
            repository.stageForPlayback(ids)
                .onSuccess {
                    val labels = ids.map { id ->
                        _state.value.entries.find { it.id == id }?.displayName.orEmpty()
                    }
                    _state.update { it.copy(isSelectionMode = false, selectedIds = emptyList()) }
                    _events.send(ReplayLibraryEvent.LaunchReplay(ids, labels))
                }
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

    companion object {
        /** Mirrors the native renderer's MAX_SWARM_VEHICLES (android_main.c). */
        const val MAX_SWARM = 16
    }
}
