package com.px4.hawkeye.feature.home.presentation

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.px4.hawkeye.core.designsystem.DronecodeGreen
import com.px4.hawkeye.core.designsystem.HawkeyeAlpha
import com.px4.hawkeye.core.designsystem.HawkeyeDimens
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.core.designsystem.MediaTitleShadow
import com.px4.hawkeye.core.designsystem.glassSurface
import com.px4.hawkeye.core.presentation.ObserveAsEvents
import com.px4.hawkeye.core.presentation.ReplayPlaybackLauncher
import com.px4.hawkeye.core.presentation.asString
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Home switches to the two-pane landscape layout once the window reaches the Material 3
 * medium-width breakpoint (600dp). Phones in landscape cross it; portrait phones do not.
 */
internal fun usesWidePane(windowSizeClass: WindowSizeClass): Boolean =
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

@Composable
fun HomeRoot(
    onNavigateToReplay: () -> Unit,
    onNavigateToLive: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
    playbackLauncher: ReplayPlaybackLauncher = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            HomeEvent.NavigateToReplay -> onNavigateToReplay()
            HomeEvent.NavigateToLive -> onNavigateToLive()
            is HomeEvent.PlayRecent -> playbackLauncher.launch(context, event.entryId)
            is HomeEvent.ShowError ->
                Toast.makeText(context, event.text.asString(context), Toast.LENGTH_SHORT).show()
        }
    }

    HomeScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun HomeScreen(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
) {
    // The looping video + scrim live in the shell (behind the nav bar too); this screen
    // is just the content layer that renders over them. statusBars + navigationBars +
    // displayCutout padding keeps content clear of the system bars and any camera cutout,
    // which sit on the side in landscape.
    val rootModifier = modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .displayCutoutPadding()
        .padding(HawkeyeDimens.screenPadding)

    if (usesWidePane(windowSizeClass)) {
        HomeWideContent(state = state, onAction = onAction, modifier = rootModifier)
    } else {
        HomeCompactContent(state = state, onAction = onAction, modifier = rootModifier)
    }
}

@Composable
private fun HomeCompactContent(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Top-aligned and scrollable: replaces the old center-aligned non-scrolling column,
    // so nothing clips on short viewports.
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        HomeHeader()
        HomeModeCards(onAction = onAction)
        if (state.recents.isNotEmpty()) {
            // RecentFlights no longer carries its own leading gap, so add the section
            // spacing here where recents stack under the cards.
            Spacer(modifier = Modifier.height(HawkeyeDimens.sectionSpacing))
            RecentFlights(
                recents = state.recents,
                onRecentClick = { onAction(HomeAction.OnRecentClicked(it)) },
            )
        }
    }
}

@Composable
private fun HomeWideContent(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        // Left column: title + subtitle at the top-left, with the two action cards beneath
        // them. Top-aligned (no centering) and scrollable as a short-viewport fallback.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
        ) {
            HomeHeader()
            HomeModeCards(onAction = onAction)
        }

        Spacer(modifier = Modifier.width(HawkeyeDimens.sectionSpacing))

        // Right column: recent flights only (capped at three, so they fit without crowding);
        // still scrollable as a safety net on unusually short viewports.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
        ) {
            if (state.recents.isNotEmpty()) {
                RecentFlights(
                    recents = state.recents,
                    onRecentClick = { onAction(HomeAction.OnRecentClicked(it)) },
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.home_title),
            // Subtle drop shadow for depth/legibility over the video.
            style = MaterialTheme.typography.headlineLarge.copy(shadow = MediaTitleShadow),
            // Fixed bright Dronecode brand green so the title stays legible over the
            // video in both light and dark themes.
            color = DronecodeGreen,
            modifier = Modifier.padding(bottom = HawkeyeDimens.titleSpacing),
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            // Light-on-video regardless of the app's light/dark theme.
            color = Color.White.copy(alpha = HawkeyeAlpha.ON_MEDIA_SECONDARY),
            modifier = Modifier.padding(bottom = HawkeyeDimens.sectionSpacing),
        )
    }
}

@Composable
private fun HomeModeCards(
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ModeCard(
            title = stringResource(R.string.home_replay_title),
            description = stringResource(R.string.home_replay_description),
            onClick = { onAction(HomeAction.OnReplayClicked) },
        )
        Spacer(modifier = Modifier.height(HawkeyeDimens.itemSpacing))
        ModeCard(
            title = stringResource(R.string.home_connect_title),
            description = stringResource(R.string.home_connect_description),
            onClick = { onAction(HomeAction.OnConnectClicked) },
        )
    }
}

@Composable
private fun RecentFlights(
    recents: List<RecentFlightUi>,
    onRecentClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // No leading gap of its own: callers add spacing when stacking it (compact layout). At
    // the top of the landscape right column it then aligns with the left-column title.
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.home_recent_header),
            style = MaterialTheme.typography.titleMedium,
            // Over the video, like the subtitle.
            color = Color.White.copy(alpha = HawkeyeAlpha.ON_MEDIA_SECONDARY),
            modifier = Modifier.padding(bottom = HawkeyeDimens.titleSpacing),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.glassSurface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = HawkeyeDimens.cardElevation),
        ) {
            Column {
                recents.forEach { recent ->
                    RecentRow(recent = recent, onClick = { onRecentClick(recent.id) })
                }
            }
        }
    }
}

@Composable
private fun RecentRow(
    recent: RecentFlightUi,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(HawkeyeDimens.cardPadding),
    ) {
        Text(
            text = recent.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = recent.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = HawkeyeAlpha.CARD_CAPTION),
            modifier = Modifier.padding(top = HawkeyeDimens.captionSpacing),
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
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            // Shared theme-aware translucent glass so the background video shows through.
            containerColor = MaterialTheme.colorScheme.glassSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = HawkeyeDimens.cardElevation),
    ) {
        Column(modifier = Modifier.padding(HawkeyeDimens.cardPadding)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(HawkeyeDimens.captionSpacing))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = HawkeyeAlpha.CARD_CAPTION),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E13)
@Composable
private fun HomeScreenPortraitPreview() {
    HawkeyeTheme(darkTheme = true) {
        HomeScreen(
            state = HomeState(
                recents = listOf(
                    RecentFlightUi("1", "flight_log.ulg", "May 30, 2026"),
                    RecentFlightUi("2", "sitl_test.ulg", "May 27, 2026"),
                ),
            ),
            onAction = {},
            windowSizeClass = WindowSizeClass(minWidthDp = 360, minHeightDp = 640),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E13, widthDp = 800, heightDp = 420)
@Composable
private fun HomeScreenLandscapePreview() {
    HawkeyeTheme(darkTheme = true) {
        HomeScreen(
            state = HomeState(
                recents = listOf(
                    RecentFlightUi("1", "flight_log.ulg", "May 30, 2026"),
                    RecentFlightUi("2", "sitl_test.ulg", "May 27, 2026"),
                ),
            ),
            onAction = {},
            windowSizeClass = WindowSizeClass(minWidthDp = 800, minHeightDp = 360),
        )
    }
}
