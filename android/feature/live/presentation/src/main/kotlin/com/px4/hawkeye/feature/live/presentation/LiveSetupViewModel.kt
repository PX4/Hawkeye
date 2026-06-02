package com.px4.hawkeye.feature.live.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.px4.hawkeye.core.domain.DeviceIpProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Pre-launch Live setup. The listen port is supplied by the shell (read from settings) so this
 * module stays free of a feature->feature dependency; the device IP comes from [DeviceIpProvider].
 * No live connection state here: the UDP socket is only bound once the renderer starts, so status
 * is surfaced in the renderer overlay, not on this screen.
 */
class LiveSetupViewModel(
    private val deviceIpProvider: DeviceIpProvider,
    listenPort: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(LiveSetupState(listenPort = listenPort))
    val state = _state.asStateFlow()

    private val _events = Channel<LiveSetupEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        refreshIp()
    }

    fun onAction(action: LiveSetupAction) {
        when (action) {
            LiveSetupAction.OnStartLiveClicked ->
                viewModelScope.launch { _events.send(LiveSetupEvent.LaunchLiveSession) }
            LiveSetupAction.OnRefreshIp -> refreshIp()
        }
    }

    private fun refreshIp() {
        val ip = deviceIpProvider.localIpAddress()
        _state.update {
            it.copy(
                deviceIp = ip,
                endpoint = if (ip != null) "udp://$ip:${it.listenPort}" else "",
            )
        }
    }
}
