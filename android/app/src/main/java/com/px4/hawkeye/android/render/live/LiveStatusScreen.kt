package com.px4.hawkeye.android.render.live

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.px4.hawkeye.android.render.LiveStatus
import com.px4.hawkeye.core.designsystem.HawkeyeDimens
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.core.designsystem.glassSurface
import com.px4.hawkeye.feature.live.domain.LiveConnectionState

/**
 * Live-mode counterpart to the replay transport bar: a non-interactive status strip hosted in
 * the same WindowManager panel above the renderer (see HawkeyeActivity). Tells the user where
 * to point PX4 while waiting, and reflects the connection once telemetry arrives.
 */
@Composable
fun LiveStatusRoot(viewModel: LiveStatusViewModel) {
    // The ViewModel owns the native-status polling loop; the Root just collects state.
    val status by viewModel.state.collectAsStateWithLifecycle()
    LiveStatusScreen(status = status, deviceIp = viewModel.deviceIp)
}

@Composable
fun LiveStatusScreen(status: LiveStatus, deviceIp: String?) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val safe = WindowInsets.safeContent
    val edgeInset = with(density) {
        maxOf(safe.getLeft(this, layoutDirection), safe.getRight(this, layoutDirection)).toDp()
    }

    val text = when (status.state) {
        LiveConnectionState.WAITING -> {
            val endpoint = if (deviceIp != null) "$deviceIp:${status.port}" else "port ${status.port}"
            stringResource(R.string.live_status_waiting, endpoint)
        }
        LiveConnectionState.CONNECTED -> stringResource(R.string.live_status_connected, status.sysid)
        LiveConnectionState.LOST -> stringResource(R.string.live_status_lost)
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
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E13, widthDp = 640)
@Composable
private fun LiveStatusWaitingPreview() {
    HawkeyeTheme(darkTheme = true) {
        LiveStatusScreen(
            status = LiveStatus(LiveConnectionState.WAITING, sysid = 0, port = 19410),
            deviceIp = "192.168.1.42",
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E13, widthDp = 640)
@Composable
private fun LiveStatusConnectedPreview() {
    HawkeyeTheme(darkTheme = true) {
        LiveStatusScreen(
            status = LiveStatus(LiveConnectionState.CONNECTED, sysid = 1, port = 19410),
            deviceIp = "192.168.1.42",
        )
    }
}
