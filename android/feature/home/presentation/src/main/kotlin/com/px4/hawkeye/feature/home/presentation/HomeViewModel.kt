package com.px4.hawkeye.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.OnReplayClicked -> viewModelScope.launch { _events.send(HomeEvent.NavigateToReplay) }
            HomeAction.OnConnectClicked -> viewModelScope.launch { _events.send(HomeEvent.NavigateToLive) }
        }
    }
}
