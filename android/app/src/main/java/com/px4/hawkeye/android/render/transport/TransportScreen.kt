package com.px4.hawkeye.android.render.transport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.px4.hawkeye.android.R
import com.px4.hawkeye.core.designsystem.HawkeyeAlpha
import com.px4.hawkeye.core.designsystem.HawkeyeDimens
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.core.designsystem.glassSurface
import java.util.Locale

/**
 * Hosted in a dedicated full-width WindowManager panel above the renderer (see
 * HawkeyeActivity), so the bar itself just fills its window — no Popup, no inset gap.
 */
@Composable
fun TransportRoot(viewModel: TransportViewModel) {
    // The ViewModel owns the native-status polling loop; the Root just collects state.
    val state by viewModel.state.collectAsStateWithLifecycle()
    TransportScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun TransportScreen(
    state: TransportState,
    onAction: (TransportAction) -> Unit,
) {
    // Nothing until a log is loaded; the host window collapses to zero height.
    if (!state.isActive) return

    // Inset the interactive controls out of the horizontal safe-area dead zones (the camera
    // cutout and the gesture-nav back-swipe strips), applied SYMMETRICALLY — the same gap on
    // both edges — so the play/pause and speed buttons sit equidistant from their respective
    // edges even when a cutout makes one side's inset larger than the other. The Surface
    // below stays full width so the bar still reads edge to edge.
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val safe = WindowInsets.safeContent
    val edgeInset = with(density) {
        maxOf(safe.getLeft(this, layoutDirection), safe.getRight(this, layoutDirection)).toDp()
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.glassSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = edgeInset)
                .padding(
                    horizontal = HawkeyeDimens.contentPadding,
                    vertical = HawkeyeDimens.rowVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HawkeyeDimens.inlineSpacing),
        ) {
            TextButton(onClick = { onAction(TransportAction.OnPlayPause) }) {
                Text(
                    text = stringResource(
                        if (state.isPaused) R.string.transport_play else R.string.transport_pause,
                    ),
                )
            }

            Text(
                text = formatTime(state.positionMs),
                style = MaterialTheme.typography.labelMedium,
            )

            Slider(
                value = fractionOf(state.positionMs, state.durationMs),
                onValueChange = { onAction(TransportAction.OnSeek(it)) },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = HawkeyeDimens.inlineSpacing),
            )

            Text(
                text = formatTime(state.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = HawkeyeAlpha.CARD_CAPTION),
            )

            TextButton(onClick = { onAction(TransportAction.OnCycleSpeed) }) {
                Text(formatSpeed(state.speed))
            }
        }
    }
}

private fun fractionOf(positionMs: Long, durationMs: Long): Float =
    if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

private fun formatSpeed(speed: Float): String =
    if (speed % 1f == 0f) "${speed.toInt()}x" else "${speed}x"

@Preview(showBackground = true, backgroundColor = 0xFF0B0E13, widthDp = 640)
@Composable
private fun TransportScreenPreview() {
    HawkeyeTheme(darkTheme = true) {
        TransportScreen(
            state = TransportState(
                isActive = true,
                isPaused = false,
                positionMs = 83_000,
                durationMs = 222_000,
                speed = 2f,
            ),
            onAction = {},
        )
    }
}
