package com.px4.hawkeye.android.shell

import android.content.res.Configuration
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.px4.hawkeye.feature.live.presentation.LiveSetupRoot
import com.px4.hawkeye.feature.replay.presentation.ReplayLibraryRoot
import com.px4.hawkeye.feature.settings.domain.AppSettings
import com.px4.hawkeye.feature.settings.domain.SettingsRepository
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import org.koin.compose.koinInject

@Composable
fun AppShell(viewModel: ShellViewModel = koinViewModel()) {
    // Nav back stacks live in a ViewModel (not `remember`) so the selected tab and the
    // per-tab back stacks survive configuration changes such as rotation.
    val backStacks = viewModel.backStacks
    val koin = getKoin()
    val installers = remember { koin.getAll<EntryProviderInstaller>() }

    val navLayoutType = navSuiteLayoutType(LocalConfiguration.current.orientation)

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
            layoutType = navLayoutType,
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
                            onConnectLive = { backStacks.push(LiveKey) },
                        )
                    }
                    // Replay and Live are pushed (non-tab) destinations, so their top bars get a
                    // back button wired to the shell back stack — registered here in :app for
                    // access to it. (Top-level tabs Home/Settings need no back button.)
                    addEntryProvider(ReplayKey::class, { it.toString() }) {
                        ReplayLibraryRoot(onBack = { backStacks.pop() })
                    }
                    addEntryProvider(LiveKey::class, { it.toString() }) {
                        val settings by koinInject<SettingsRepository>()
                            .settings.collectAsStateWithLifecycle(AppSettings())
                        LiveSetupRoot(
                            listenPort = settings.listenPort,
                            onBack = { backStacks.pop() },
                        )
                    }
                    installers.forEach { it() }
                },
            )
        }
    }
}

/**
 * Pick the navigation-suite layout for the current [orientation]. In landscape a bottom
 * navigation bar eats scarce vertical space and clips content on short phone viewports, so
 * use a side navigation rail there; keep the bottom bar in portrait where height is plentiful.
 */
internal fun navSuiteLayoutType(orientation: Int): NavigationSuiteType =
    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        NavigationSuiteType.NavigationRail
    } else {
        NavigationSuiteType.NavigationBar
    }

@StringRes
private fun TopLevelDestination.labelRes(): Int = when (this) {
    TopLevelDestination.HOME -> R.string.shell_nav_home
    TopLevelDestination.SETTINGS -> R.string.shell_nav_settings
}
