package com.px4.hawkeye.android.shell

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.px4.hawkeye.core.designsystem.glassSurface
import com.px4.hawkeye.core.navigation.EntryProviderInstaller
import com.px4.hawkeye.core.navigation.HomeKey
import com.px4.hawkeye.core.navigation.LiveKey
import com.px4.hawkeye.core.navigation.NavBackStacks
import com.px4.hawkeye.core.navigation.ReplayKey
import com.px4.hawkeye.core.navigation.TopLevelDestination
import com.px4.hawkeye.feature.home.presentation.HomeRoot
import com.px4.hawkeye.feature.home.presentation.HomeVideoBackground
import org.koin.compose.getKoin

@Composable
fun AppShell() {
    val backStacks = remember { NavBackStacks(TopLevelDestination.HOME) }
    val koin = getKoin()
    val installers = remember { koin.getAll<EntryProviderInstaller>() }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Looping video (+ a legibility scrim) behind the entire Home screen, including
        // the nav bar, shown only while the Home tab is active. The translucent nav bar
        // and cards let it show through. Released when leaving the Home tab.
        if (backStacks.selected == TopLevelDestination.HOME) {
            HomeVideoBackground(modifier = Modifier.matchParentSize())
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
            )
        }

        NavigationSuiteScaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            navigationSuiteColors = NavigationSuiteDefaults.colors(
                navigationBarContainerColor = MaterialTheme.colorScheme.glassSurface,
                navigationRailContainerColor = MaterialTheme.colorScheme.glassSurface,
            ),
            navigationSuiteItems = {
                TopLevelDestination.entries.forEach { dest ->
                    item(
                        selected = backStacks.selected == dest,
                        onClick = { backStacks.select(dest) },
                        icon = { Text(if (dest == TopLevelDestination.HOME) "⌂" else "⚙") },
                        label = { Text(dest.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            },
        ) {
        // Directional slide: forward = new enters from right, old exits left;
        // backward = new enters from left, old exits right.
        val directionalSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
            if (backStacks.transitionForward) {
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            } else {
                (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
            }
        }

        NavDisplay(
            backStack = backStacks.current,
            onBack = { backStacks.pop() },
            transitionSpec = directionalSpec,
            popTransitionSpec = directionalSpec,
            predictivePopTransitionSpec = {
                (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
            },
            entryProvider = entryProvider {
                addEntryProvider(HomeKey::class, { it.toString() }) {
                    HomeRoot(
                        onNavigateToReplay = { backStacks.push(ReplayKey) },
                        onNavigateToLive = { backStacks.push(LiveKey) },
                    )
                }
                addEntryProvider(ReplayKey::class, { it.toString() }) {
                    ComingSoonScreen(
                        title = "Replay",
                        message = "The replay library and file picker are coming in Plan 2.",
                        onBack = { backStacks.pop() },
                    )
                }
                addEntryProvider(LiveKey::class, { it.toString() }) {
                    ComingSoonScreen(
                        title = "Live / SITL",
                        message = "Connecting to a simulator is coming in Plan 3.",
                        onBack = { backStacks.pop() },
                    )
                }
                installers.forEach { it() }
            },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComingSoonScreen(
    title: String,
    message: String,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(message)
        }
    }
}
