package com.px4.hawkeye.android.shell

import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.px4.hawkeye.android.R
import com.px4.hawkeye.core.designsystem.ScrimColor
import com.px4.hawkeye.core.designsystem.glassSurface
import com.px4.hawkeye.core.navigation.EntryProviderInstaller
import com.px4.hawkeye.core.navigation.HomeKey
import com.px4.hawkeye.core.navigation.LiveKey
import com.px4.hawkeye.core.navigation.ReplayKey
import com.px4.hawkeye.core.navigation.TopLevelDestination
import com.px4.hawkeye.feature.home.presentation.HomeRoot
import com.px4.hawkeye.feature.home.presentation.HomeVideoBackground
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin

@Composable
fun AppShell(viewModel: ShellViewModel = koinViewModel()) {
    // Nav back stacks live in a ViewModel (not `remember`) so the selected tab and the
    // per-tab back stacks survive configuration changes such as rotation.
    val backStacks = viewModel.backStacks
    val koin = getKoin()
    val installers = remember { koin.getAll<EntryProviderInstaller>() }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Looping video (+ a legibility scrim) behind the entire Home screen, including the
        // nav bar. Shown only while Home is the selected tab AND at the root of its back
        // stack: navigating deeper (Replay/Live render over an opaque surface) releases the
        // ExoPlayer via HomeVideoBackground's DisposableEffect instead of decoding unseen.
        if (backStacks.selected == TopLevelDestination.HOME &&
            backStacks.current.lastOrNull() == HomeKey
        ) {
            HomeVideoBackground(modifier = Modifier.matchParentSize())
            Box(modifier = Modifier.matchParentSize().background(ScrimColor))
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
                        label = { Text(stringResource(dest.labelRes())) },
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
                    // Replay's NavEntry is contributed by the feature's EntryProviderInstaller
                    // (collected into `installers` below). Live is still a placeholder.
                    addEntryProvider(LiveKey::class, { it.toString() }) {
                        ComingSoonScreen(
                            titleRes = R.string.shell_live_title,
                            messageRes = R.string.shell_live_coming_soon,
                            onBack = { backStacks.pop() },
                        )
                    }
                    installers.forEach { it() }
                },
            )
        }
    }
}

@StringRes
private fun TopLevelDestination.labelRes(): Int = when (this) {
    TopLevelDestination.HOME -> R.string.shell_nav_home
    TopLevelDestination.SETTINGS -> R.string.shell_nav_settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComingSoonScreen(
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.shell_back)) }
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
            Text(stringResource(messageRes))
        }
    }
}
