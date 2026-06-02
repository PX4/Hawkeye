package com.px4.hawkeye.android.render.live

import androidx.lifecycle.ViewModel
import com.px4.hawkeye.android.render.LiveStatus
import com.px4.hawkeye.android.render.LiveStatusController
import com.px4.hawkeye.feature.live.domain.LiveConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Backs the live-status overlay. Pull-based snapshot of the native receiver: [refresh] reads
 * the current status (the Root polls it on a cadence). Synchronous and unit-testable; the
 * device IP is supplied by the host (it's a JVM-side value, not known to native).
 */
class LiveStatusViewModel(
    private val controller: LiveStatusController,
    deviceIp: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(
        LiveStatus(LiveConnectionState.WAITING, sysid = 0, port = 0),
    )
    val state = _state.asStateFlow()

    val deviceIp: String? = deviceIp

    fun refresh() {
        _state.value = controller.status()
    }

    companion object {
        const val POLL_INTERVAL_MS = 250L
    }
}
