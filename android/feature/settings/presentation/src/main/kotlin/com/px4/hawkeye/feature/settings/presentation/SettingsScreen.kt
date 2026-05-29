package com.px4.hawkeye.feature.settings.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.px4.hawkeye.feature.settings.domain.DistanceUnit
import com.px4.hawkeye.feature.settings.domain.ThemeMode
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsRoot(viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun SettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        ThemeMode.entries.forEach { mode ->
            OptionRow(
                label = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                selected = state.themeMode == mode,
                onClick = { onAction(SettingsAction.OnThemeModeSelected(mode)) },
            )
        }

        Text(
            text = "Units",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        DistanceUnit.entries.forEach { unit ->
            OptionRow(
                label = unit.name.lowercase().replaceFirstChar { it.uppercase() },
                selected = state.distanceUnit == unit,
                onClick = { onAction(SettingsAction.OnDistanceUnitSelected(unit)) },
            )
        }
    }
}

@Composable
private fun OptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 4.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
