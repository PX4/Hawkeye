package com.px4.hawkeye.feature.live.presentation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.px4.hawkeye.core.designsystem.HawkeyeDimens
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.core.designsystem.glassSurface
import com.px4.hawkeye.core.presentation.LivePlaybackLauncher
import com.px4.hawkeye.core.presentation.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun LiveSetupRoot(
    listenPort: Int,
    onBack: () -> Unit,
    viewModel: LiveSetupViewModel = koinViewModel { parametersOf(listenPort) },
    liveLauncher: LivePlaybackLauncher = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Raising the system prompt needs an activity result launcher, which only a composable
    // can own. Whether to raise it is the ViewModel's call; this just carries the answer back.
    val localNetworkPrompt = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onAction(LiveSetupAction.OnLocalNetworkPermissionResult(granted))
    }

    // A grant made on the system settings page comes back through no result callback, so
    // the screen re-asks on every resume to retire a notice the user has already acted on.
    LifecycleResumeEffect(Unit) {
        viewModel.onAction(LiveSetupAction.OnResumed)
        onPauseOrDispose { }
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            LiveSetupEvent.LaunchLiveSession -> liveLauncher.launch(context, state.listenPort)

            LiveSetupEvent.RequestLocalNetworkPermission ->
                localNetworkPrompt.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)

            // runCatching, because this is the recovery path: a device policy can disable
            // the Settings app, and an uncaught ActivityNotFoundException here would crash
            // the very screen the user came to in order to fix something. NEW_TASK so it
            // still resolves if LocalContext is ever not an Activity.
            LiveSetupEvent.OpenAppSettings -> runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    LiveSetupScreen(state = state, onAction = viewModel::onAction, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSetupScreen(
    state: LiveSetupState,
    onAction: (LiveSetupAction) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.live_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.live_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(HawkeyeDimens.contentPadding),
        ) {
            Text(
                text = stringResource(R.string.live_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(HawkeyeDimens.itemSpacing))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.glassSurface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = HawkeyeDimens.cardElevation),
            ) {
                Column(modifier = Modifier.padding(HawkeyeDimens.cardPadding)) {
                    if (state.endpoint.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.live_endpoint_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = state.endpoint,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.live_ip_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = { onAction(LiveSetupAction.OnRefreshIp) }) {
                            Text(stringResource(R.string.live_retry_ip))
                        }
                    }
                    Spacer(modifier = Modifier.height(HawkeyeDimens.captionSpacing))
                    Text(
                        text = stringResource(R.string.live_port_label, state.listenPort),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(HawkeyeDimens.itemSpacing))
            Text(
                text = stringResource(R.string.live_change_port_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.localNetworkDenied) {
                Spacer(modifier = Modifier.height(HawkeyeDimens.itemSpacing))
                Text(
                    text = stringResource(R.string.live_local_network_denied),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = { onAction(LiveSetupAction.OnOpenAppSettingsClicked) }) {
                    Text(stringResource(R.string.live_open_settings))
                }
            }

            Spacer(modifier = Modifier.height(HawkeyeDimens.sectionSpacing))
            Button(
                onClick = { onAction(LiveSetupAction.OnStartLiveClicked) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.live_start_button))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LiveSetupScreenPreview() {
    HawkeyeTheme {
        LiveSetupScreen(
            state = LiveSetupState(
                deviceIp = "192.168.1.42",
                listenPort = 19410,
                endpoint = "udp://192.168.1.42:19410",
            ),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LiveSetupScreenPermissionDeniedPreview() {
    HawkeyeTheme {
        LiveSetupScreen(
            state = LiveSetupState(
                deviceIp = "192.168.1.42",
                listenPort = 19410,
                endpoint = "udp://192.168.1.42:19410",
                localNetworkDenied = true,
            ),
            onAction = {},
            onBack = {},
        )
    }
}
