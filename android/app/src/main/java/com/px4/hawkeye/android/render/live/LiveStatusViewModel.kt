package com.px4.hawkeye.android.render.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.px4.hawkeye.android.render.LiveStatus
import com.px4.hawkeye.android.render.LiveStatusController
import com.px4.hawkeye.feature.live.domain.LiveConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Backs the live-status overlay. Pull-based snapshot of the native receiver, which has no push
 * channel: the ViewModel owns the polling cadence ([refresh] on a [viewModelScope] loop) and the
 * Root just collects [state]. The device IP is supplied by the host (a JVM-side value not known
 * to native).
 */
class LiveStatusViewModel(
    private val controller: LiveStatusController,
    val deviceIp: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(
        LiveStatus(LiveConnectionState.WAITING, sysid = 0, port = 0),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun refresh() {
        _state.value = controller.status()
    }

    companion object {
        const val POLL_INTERVAL_MS = 250L
    }
}
