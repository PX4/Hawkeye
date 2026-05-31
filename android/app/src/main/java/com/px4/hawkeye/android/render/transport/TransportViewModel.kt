package com.px4.hawkeye.android.render.transport

import androidx.lifecycle.ViewModel
import com.px4.hawkeye.android.render.ReplayController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Backs the touch transport overlay. State is a pull-based snapshot of the native engine:
 * [refresh] reads the current status (the Root polls it on a cadence), and [onAction]
 * forwards user input to the [controller]. No coroutines/`viewModelScope` here — the
 * polling cadence lives in the Root as a lifecycle side effect, which keeps this fully
 * synchronous and unit-testable.
 */
class TransportViewModel(
    private val controller: ReplayController,
) : ViewModel() {

    private val _state = MutableStateFlow(TransportState())
    val state = _state.asStateFlow()

    fun refresh() {
        val status = controller.status()
        _state.update {
            it.copy(
                isActive = status.active,
                isPaused = status.paused,
                positionMs = (status.positionS * MILLIS).toLong(),
                durationMs = (status.durationS * MILLIS).toLong(),
                speed = status.speed,
            )
        }
    }

    fun onAction(action: TransportAction) {
        when (action) {
            TransportAction.OnPlayPause -> {
                val paused = !_state.value.isPaused
                controller.setPaused(paused)
                _state.update { it.copy(isPaused = paused) }
            }
            is TransportAction.OnSeek -> {
                val durationS = _state.value.durationMs / MILLIS
                controller.seekTo(action.fraction.coerceIn(0f, 1f) * durationS)
            }
            TransportAction.OnCycleSpeed -> {
                val next = nextSpeed(_state.value.speed)
                controller.setSpeed(next)
                _state.update { it.copy(speed = next) }
            }
        }
    }

    private fun nextSpeed(current: Float): Float {
        val index = SPEEDS.indexOfFirst { it >= current - 0.01f }.coerceAtLeast(0)
        return SPEEDS[(index + 1) % SPEEDS.size]
    }

    companion object {
        const val POLL_INTERVAL_MS = 100L
        private const val MILLIS = 1000f
        private val SPEEDS = listOf(0.5f, 1f, 2f, 4f)
    }
}
