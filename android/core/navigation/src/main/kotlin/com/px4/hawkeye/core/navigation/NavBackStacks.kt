package com.px4.hawkeye.core.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey

/**
 * Owns one back stack per top-level destination and tracks which is selected.
 * Backed by snapshot state so Compose recomposes when it mutates. The shell
 * feeds [current] to a single NavDisplay.
 */
class NavBackStacks(startTab: TopLevelDestination) {

    private val stacks: Map<TopLevelDestination, SnapshotStateList<NavKey>> =
        TopLevelDestination.entries.associateWith { mutableStateListOf(it.key) }

    var selected by mutableStateOf(startTab)
        private set

    val current: List<NavKey> get() = stacks.getValue(selected)

    /**
     * Direction of the most recent navigation. `true` = forward (entering a higher-index
     * tab or pushing onto the stack); `false` = backward (going to a lower-index tab,
     * reselecting the active tab, or popping). Defaults to `true`. Not snapshot state —
     * read by the shell at the moment a transition starts.
     */
    var transitionForward: Boolean = true
        private set

    fun push(key: NavKey) {
        transitionForward = true
        stacks.getValue(selected).add(key)
    }

    fun pop() {
        transitionForward = false
        val stack = stacks.getValue(selected)
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    /** Switch tabs; reselecting the active tab pops it back to its root. */
    fun select(tab: TopLevelDestination) {
        if (tab == selected) {
            transitionForward = false
            val stack = stacks.getValue(tab)
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
        } else {
            transitionForward = tab.ordinal > selected.ordinal
            selected = tab
        }
    }
}
