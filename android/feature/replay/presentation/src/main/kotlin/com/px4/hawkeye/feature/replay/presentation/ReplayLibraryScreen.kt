package com.px4.hawkeye.feature.replay.presentation

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.activity.compose.BackHandler
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
    onBack: () -> Unit,
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
            is ReplayLibraryEvent.LaunchReplay -> playbackLauncher.launch(context, event.droneLabels)
            is ReplayLibraryEvent.ShowError ->
                Toast.makeText(context, event.text.asString(context), Toast.LENGTH_SHORT).show()
        }
    }

    ReplayLibraryScreen(state = state, onAction = viewModel::onAction, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplayLibraryScreen(
    state: ReplayLibraryState,
    onAction: (ReplayLibraryAction) -> Unit,
    onBack: () -> Unit,
) {
    // System back leaves selection mode first (standard selection UX); only a second
    // back navigates away from the library.
    BackHandler(enabled = state.isSelectionMode) {
        onAction(ReplayLibraryAction.OnToggleSelectionMode)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isSelectionMode) {
                            stringResource(R.string.replay_selection_title, state.selectedIds.size)
                        } else {
                            stringResource(R.string.replay_library_title)
                        },
                    )
                },
                navigationIcon = {
                    if (state.isSelectionMode) {
                        IconButton(onClick = { onAction(ReplayLibraryAction.OnToggleSelectionMode) }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.replay_exit_selection),
                            )
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.replay_back),
                            )
                        }
                    }
                },
                actions = {
                    if (!state.isSelectionMode && state.entries.size >= 2) {
                        TextButton(onClick = { onAction(ReplayLibraryAction.OnToggleSelectionMode) }) {
                            Text(stringResource(R.string.replay_select))
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.isSelectionMode) {
                if (state.selectedIds.size >= 2) {
                    ExtendedFloatingActionButton(
                        onClick = { onAction(ReplayLibraryAction.OnPlayTogetherClicked) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) { Text(stringResource(R.string.replay_play_together, state.selectedIds.size)) }
                }
            } else {
                ExtendedFloatingActionButton(
                    onClick = { onAction(ReplayLibraryAction.OnOpenFileClicked) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) { Text(stringResource(R.string.replay_open_file)) }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                state.entries.isEmpty() ->
                    EmptyState(modifier = Modifier.align(Alignment.Center))

                else -> LibraryList(
                    entries = state.entries,
                    isSelectionMode = state.isSelectionMode,
                    selectedIds = state.selectedIds,
                    onAction = onAction,
                )
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
    isSelectionMode: Boolean,
    selectedIds: List<String>,
    onAction: (ReplayLibraryAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = HawkeyeDimens.contentPadding),
    ) {
        items(items = entries, key = { it.id }) { entry ->
            LibraryRow(
                entry = entry,
                isSelectionMode = isSelectionMode,
                isSelected = entry.id in selectedIds,
                onClick = { onAction(ReplayLibraryAction.OnEntryClicked(entry.id)) },
                // Long-press keeps its delete meaning only outside selection mode, so a
                // sloppy selection tap can never surface the destructive dialog.
                onLongClick = {
                    if (!isSelectionMode) onAction(ReplayLibraryAction.OnDeleteRequested(entry.id))
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryRow(
    entry: LibraryEntryUi,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(
                horizontal = HawkeyeDimens.contentPadding,
                vertical = HawkeyeDimens.itemSpacing,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                // Row-level combinedClickable owns the toggle so checkbox and row behave
                // identically; a null handler keeps the checkbox purely visual.
                onCheckedChange = null,
                modifier = Modifier.padding(end = HawkeyeDimens.inlineSpacing),
            )
        }
        Column {
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
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReplayLibrarySelectionPreview() {
    HawkeyeTheme {
        ReplayLibraryScreen(
            state = ReplayLibraryState(
                isLoading = false,
                isSelectionMode = true,
                selectedIds = listOf("1", "2"),
                entries = listOf(
                    LibraryEntryUi("1", "flight_2026_05_28.ulg", "12.4 MB", "May 28, 2026"),
                    LibraryEntryUi("2", "sitl_test.ulg", "3.1 MB", "May 27, 2026"),
                    LibraryEntryUi("3", "hover_check.ulg", "1.8 MB", "May 26, 2026"),
                ),
            ),
            onAction = {},
            onBack = {},
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
            onBack = {},
        )
    }
}
