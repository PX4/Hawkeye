package com.px4.hawkeye.feature.live.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.px4.hawkeye.core.domain.DeviceIpProvider
import com.px4.hawkeye.core.domain.LocalNetworkPermission
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
 *
 * A live session cannot work without local network access, so the decision to start one or ask
 * for permission first lives here rather than in the composable, behind [LocalNetworkPermission].
 * The host only ever executes the resulting event: it owns the prompt, because raising one needs
 * an activity result launcher, but it decides nothing.
 */
class LiveSetupViewModel(
    private val deviceIpProvider: DeviceIpProvider,
    private val localNetworkPermission: LocalNetworkPermission,
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
            LiveSetupAction.OnStartLiveClicked -> startOrRequestPermission()
            LiveSetupAction.OnRefreshIp -> refreshIp()

            // Derived from the result rather than only set on refusal, so the notice can
            // never outlive the condition it describes.
            is LiveSetupAction.OnLocalNetworkPermissionResult -> {
                _state.update { it.copy(localNetworkDenied = !action.granted) }
                if (action.granted) send(LiveSetupEvent.LaunchLiveSession)
            }

            LiveSetupAction.OnOpenAppSettingsClicked -> send(LiveSetupEvent.OpenAppSettings)

            // Only ever clears. Resuming must not be able to invent a refusal the user
            // never made, which is what would happen on first composition otherwise.
            LiveSetupAction.OnResumed ->
                if (localNetworkPermission.isGranted()) {
                    _state.update { it.copy(localNetworkDenied = false) }
                }
        }
    }

    private fun startOrRequestPermission() {
        if (localNetworkPermission.isGranted()) {
            _state.update { it.copy(localNetworkDenied = false) }
            send(LiveSetupEvent.LaunchLiveSession)
        } else {
            send(LiveSetupEvent.RequestLocalNetworkPermission)
        }
    }

    private fun send(event: LiveSetupEvent) {
        viewModelScope.launch { _events.send(event) }
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
