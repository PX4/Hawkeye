package com.px4.hawkeye.core.navigation

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class NavBackStacksTest {

    @Test
    fun `starts with each top-level root and HOME selected`() {
        val stacks = NavBackStacks(TopLevelDestination.HOME)
        assertThat(stacks.selected).isEqualTo(TopLevelDestination.HOME)
        assertThat(stacks.current).containsExactly(HomeKey)
    }

    @Test
    fun `push adds to the selected stack`() {
        val stacks = NavBackStacks(TopLevelDestination.HOME)
        stacks.push(ReplayKey)
        assertThat(stacks.current).containsExactly(HomeKey, ReplayKey)
    }

    @Test
    fun `pop removes the top of the selected stack`() {
        val stacks = NavBackStacks(TopLevelDestination.HOME)
        stacks.push(ReplayKey)
        stacks.pop()
        assertThat(stacks.current).containsExactly(HomeKey)
    }

    @Test
    fun `pop on a single-entry root is a no-op`() {
        val stacks = NavBackStacks(TopLevelDestination.HOME)
        stacks.pop()
        assertThat(stacks.current).containsExactly(HomeKey)
    }

    @Test
    fun `selecting a tab switches stacks and preserves each history`() {
        val stacks = NavBackStacks(TopLevelDestination.HOME)
        stacks.push(ReplayKey)
        stacks.select(TopLevelDestination.SETTINGS)
        assertThat(stacks.current).containsExactly(SettingsKey)
        stacks.select(TopLevelDestination.HOME)
        assertThat(stacks.current).containsExactly(HomeKey, ReplayKey)
    }

    @Test
    fun `reselecting the active tab pops to its root`() {
        val stacks = NavBackStacks(TopLevelDestination.HOME)
        stacks.push(ReplayKey)
        stacks.select(TopLevelDestination.HOME)
        assertThat(stacks.current).containsExactly(HomeKey)
    }

    @Test
    fun `selecting a higher-index tab sets transitionForward true`() {
        val stacks = NavBackStacks(TopLevelDestination.HOME)
        stacks.select(TopLevelDestination.SETTINGS) // HOME(0) -> SETTINGS(1)
        assertThat(stacks.transitionForward).isTrue()
    }

    @Test
    fun `selecting a lower-index tab sets transitionForward false`() {
        val stacks = NavBackStacks(TopLevelDestination.HOME)
        stacks.select(TopLevelDestination.SETTINGS)
        stacks.select(TopLevelDestination.HOME) // SETTINGS(1) -> HOME(0)
        assertThat(stacks.transitionForward).isFalse()
    }

    @Test
    fun `push sets transitionForward true`() {
        val stacks = NavBackStacks(TopLevelDestination.HOME)
        stacks.select(TopLevelDestination.SETTINGS)
        stacks.select(TopLevelDestination.HOME)
        stacks.push(ReplayKey)
        assertThat(stacks.transitionForward).isTrue()
    }

    @Test
    fun `pop sets transitionForward false`() {
        val stacks = NavBackStacks(TopLevelDestination.HOME)
        stacks.push(ReplayKey)
        stacks.pop()
        assertThat(stacks.transitionForward).isFalse()
    }

    @Test
    fun `transitionForward defaults to true`() {
        val stacks = NavBackStacks(TopLevelDestination.HOME)
        assertThat(stacks.transitionForward).isTrue()
    }

    @Test
    fun `reselecting the active tab sets transitionForward false`() {
        val stacks = NavBackStacks(TopLevelDestination.HOME)
        stacks.select(TopLevelDestination.HOME) // reselect current tab
        assertThat(stacks.transitionForward).isFalse()
    }
}
