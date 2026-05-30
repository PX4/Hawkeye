package com.px4.hawkeye.android.shell

import androidx.lifecycle.ViewModel
import com.px4.hawkeye.core.navigation.NavBackStacks
import com.px4.hawkeye.core.navigation.TopLevelDestination

/**
 * Owns the shell's navigation back stacks. Held in a ViewModel (not `remember`) so the
 * selected tab and per-tab back stacks survive configuration changes such as rotation:
 * the shell rotates freely and `MainActivity` does not declare `configChanges`, so it is
 * recreated on rotation while this ViewModel persists.
 */
class ShellViewModel : ViewModel() {
    val backStacks = NavBackStacks(TopLevelDestination.HOME)
}
