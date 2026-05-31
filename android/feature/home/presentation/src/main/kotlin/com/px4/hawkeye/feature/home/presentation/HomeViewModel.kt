package com.px4.hawkeye.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.px4.hawkeye.core.domain.LibraryEntry
import com.px4.hawkeye.core.domain.ReplayLibraryRepository
import com.px4.hawkeye.core.domain.onFailure
import com.px4.hawkeye.core.domain.onSuccess
import com.px4.hawkeye.core.presentation.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: ReplayLibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.observeLibrary().collect { entries ->
                _state.update { it.copy(recents = entries.take(MAX_RECENTS).map(LibraryEntry::toRecentUi)) }
            }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.OnReplayClicked -> viewModelScope.launch { _events.send(HomeEvent.NavigateToReplay) }
            HomeAction.OnConnectClicked -> viewModelScope.launch { _events.send(HomeEvent.NavigateToLive) }
            is HomeAction.OnRecentClicked -> playRecent(action.id)
        }
    }

    private fun playRecent(id: String) {
        viewModelScope.launch {
            repository.stageForPlayback(id)
                .onSuccess { _events.send(HomeEvent.PlayRecent(id)) }
                .onFailure { _events.send(HomeEvent.ShowError(it.toUiText())) }
        }
    }

    private companion object {
        const val MAX_RECENTS = 3
    }
}
