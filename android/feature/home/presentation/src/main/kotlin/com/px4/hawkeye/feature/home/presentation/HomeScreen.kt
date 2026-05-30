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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.px4.hawkeye.core.designsystem.DronecodeGreen
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.core.designsystem.glassSurface
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
    // The looping video + scrim live in the shell (behind the nav bar too); this screen
    // is just the content layer that renders over them.
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Hawkeye",
            // Subtle drop shadow for depth/legibility over the video.
            style = MaterialTheme.typography.headlineLarge.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.6f),
                    offset = Offset(0f, 2f),
                    blurRadius = 6f,
                ),
            ),
            // Fixed bright Dronecode brand green so the title stays legible over the
            // video in both light and dark themes.
            color = DronecodeGreen,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            text = "Select a mode to get started",
            style = MaterialTheme.typography.bodyMedium,
            // Light-on-video regardless of the app's light/dark theme.
            color = Color.White.copy(alpha = 0.85f),
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
            // Shared theme-aware translucent glass so the background video shows through.
            containerColor = MaterialTheme.colorScheme.glassSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                // Theme-driven: the glass card is light in light mode, dark in dark mode.
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
