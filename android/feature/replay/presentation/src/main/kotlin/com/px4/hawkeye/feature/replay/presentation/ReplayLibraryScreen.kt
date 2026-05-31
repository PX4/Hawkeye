package com.px4.hawkeye.feature.replay.presentation

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.px4.hawkeye.core.designsystem.HawkeyeAlpha
import com.px4.hawkeye.core.designsystem.HawkeyeDimens
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.core.presentation.ObserveAsEvents
import com.px4.hawkeye.core.presentation.ReplayPlaybackLauncher
import com.px4.hawkeye.core.presentation.asString
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun ReplayLibraryRoot(
    viewModel: ReplayLibraryViewModel = koinViewModel(),
    playbackLauncher: ReplayPlaybackLauncher = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ULog has no registered MIME, so accept any document and validate on import.
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.onAction(ReplayLibraryAction.OnFilePicked(uri?.toString()))
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ReplayLibraryEvent.LaunchFilePicker -> pickFile.launch(arrayOf("*/*"))
            is ReplayLibraryEvent.LaunchReplay -> playbackLauncher.launch(context, event.entryId)
            is ReplayLibraryEvent.ShowError ->
                Toast.makeText(context, event.text.asString(context), Toast.LENGTH_SHORT).show()
        }
    }

    ReplayLibraryScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplayLibraryScreen(
    state: ReplayLibraryState,
    onAction: (ReplayLibraryAction) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.replay_library_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction(ReplayLibraryAction.OnOpenFileClicked) },
            ) { Text(stringResource(R.string.replay_open_file)) }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                state.entries.isEmpty() ->
                    EmptyState(modifier = Modifier.align(Alignment.Center))

                else -> LibraryList(entries = state.entries, onAction = onAction)
            }

            if (state.isImporting) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                )
            }
        }
    }

    state.pendingDelete?.let { entry ->
        DeleteConfirmationDialog(
            displayName = entry.displayName,
            onConfirm = { onAction(ReplayLibraryAction.OnConfirmDelete) },
            onDismiss = { onAction(ReplayLibraryAction.OnDismissDelete) },
        )
    }
}

@Composable
private fun LibraryList(
    entries: List<LibraryEntryUi>,
    onAction: (ReplayLibraryAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = HawkeyeDimens.contentPadding),
    ) {
        items(items = entries, key = { it.id }) { entry ->
            LibraryRow(
                entry = entry,
                onClick = { onAction(ReplayLibraryAction.OnEntryClicked(entry.id)) },
                onLongClick = { onAction(ReplayLibraryAction.OnDeleteRequested(entry.id)) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryRow(
    entry: LibraryEntryUi,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(
                horizontal = HawkeyeDimens.contentPadding,
                vertical = HawkeyeDimens.itemSpacing,
            ),
    ) {
        Text(
            text = entry.displayName,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.replay_entry_meta, entry.sizeLabel, entry.importedLabel),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = HawkeyeAlpha.CARD_CAPTION),
            modifier = Modifier.padding(top = HawkeyeDimens.captionSpacing),
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(HawkeyeDimens.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HawkeyeDimens.titleSpacing),
    ) {
        Text(
            text = stringResource(R.string.replay_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.replay_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = HawkeyeAlpha.CARD_CAPTION),
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    displayName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.replay_delete_title)) },
        text = { Text(stringResource(R.string.replay_delete_message, displayName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.replay_delete_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.replay_delete_cancel)) }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun ReplayLibraryScreenPreview() {
    HawkeyeTheme {
        ReplayLibraryScreen(
            state = ReplayLibraryState(
                isLoading = false,
                entries = listOf(
                    LibraryEntryUi("1", "flight_2026_05_28.ulg", "12.4 MB", "May 28, 2026"),
                    LibraryEntryUi("2", "sitl_test.ulg", "3.1 MB", "May 27, 2026"),
                ),
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReplayLibraryEmptyPreview() {
    HawkeyeTheme {
        ReplayLibraryScreen(
            state = ReplayLibraryState(isLoading = false),
            onAction = {},
        )
    }
}
