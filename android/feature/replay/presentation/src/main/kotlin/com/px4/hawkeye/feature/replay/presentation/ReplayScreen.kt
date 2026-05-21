package com.px4.hawkeye.feature.replay.presentation

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.core.presentation.ObserveAsEvents
import com.px4.hawkeye.core.presentation.asString
import org.koin.androidx.compose.koinViewModel

@Composable
fun ReplayRoot(viewModel: ReplayViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ReplayEvent.ShowToast ->
                Toast.makeText(context, event.text.asString(context), Toast.LENGTH_SHORT).show()
        }
    }

    ReplayScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun ReplayScreen(
    state: ReplayState,
    onAction: (ReplayAction) -> Unit
) {
    when (val dialog = state.dialog) {
        is ReplayDialog.ConfirmOpen -> ConfirmOpenDialog(dialog, onAction)
        ReplayDialog.NoFileLoaded -> NoFileLoadedDialog(onAction)
        null -> Unit
    }
}

@Composable
private fun ConfirmOpenDialog(
    dialog: ReplayDialog.ConfirmOpen,
    onAction: (ReplayAction) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onAction(ReplayAction.OnDismissDialog) },
        title = { Text(stringResource(R.string.replay_prompt_open_title)) },
        text = {
            Text(
                text = stringResource(
                    R.string.replay_prompt_open_message,
                    dialog.displayName,
                    dialog.source
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onAction(ReplayAction.OnConfirmOpen) }) {
                Text(stringResource(R.string.replay_prompt_open))
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(ReplayAction.OnDismissDialog) }) {
                Text(stringResource(R.string.replay_prompt_cancel))
            }
        }
    )
}

@Composable
private fun NoFileLoadedDialog(onAction: (ReplayAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(ReplayAction.OnDismissDialog) },
        title = { Text(stringResource(R.string.replay_prompt_no_file_title)) },
        text = { Text(stringResource(R.string.replay_prompt_no_file_message)) },
        confirmButton = {
            TextButton(onClick = { onAction(ReplayAction.OnDismissDialog) }) {
                Text(stringResource(R.string.replay_prompt_ok))
            }
        }
    )
}

@Preview
@Composable
private fun ConfirmOpenDialogPreview() {
    HawkeyeTheme {
        ReplayScreen(
            state = ReplayState(
                dialog = ReplayDialog.ConfirmOpen(
                    displayName = "flight_2025_05_21.ulg",
                    source = "com.google.android.apps.docs.storage"
                )
            ),
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun NoFileLoadedDialogPreview() {
    HawkeyeTheme {
        ReplayScreen(
            state = ReplayState(dialog = ReplayDialog.NoFileLoaded),
            onAction = {}
        )
    }
}
