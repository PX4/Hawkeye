package com.px4.hawkeye.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.core.presentation.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeRoot(
    onNavigateToReplay: () -> Unit,
    onNavigateToLive: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            HomeEvent.NavigateToReplay -> onNavigateToReplay()
            HomeEvent.NavigateToLive -> onNavigateToLive()
        }
    }

    HomeScreen(onAction = viewModel::onAction)
}

@Composable
fun HomeScreen(
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Hawkeye",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            text = "Select a mode to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 32.dp),
        )

        ModeCard(
            title = "Replay a flight",
            description = "Load a ULog file and replay recorded flight data",
            onClick = { onAction(HomeAction.OnReplayClicked) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        ModeCard(
            title = "Connect to a simulator",
            description = "Stream live telemetry from PX4 SITL or a real vehicle",
            onClick = { onAction(HomeAction.OnConnectClicked) },
        )
    }
}

@Composable
private fun ModeCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E13)
@Composable
private fun HomeScreenPreview() {
    HawkeyeTheme(darkTheme = true) {
        HomeScreen(onAction = {})
    }
}
