package com.px4.hawkeye.core.designsystem.wheel

import androidx.compose.ui.graphics.Color
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class WheelMenuNavigatorTest {

    private fun leaf(label: String) =
        WheelMenuItem(label = label, accentColor = Color.White, id = label)

    private fun parent(label: String, children: List<WheelMenuItem>) =
        WheelMenuItem(label = label, accentColor = Color.White, children = children)

    private val viewLeaves = listOf(leaf("Chase"), leaf("FPV"), leaf("Top"))
    private val droneLeaves = listOf(leaf("Drone 1"), leaf("Drone 2"))

    private fun root() = listOf(
        parent("Change View", viewLeaves),
        parent("Select Drone", droneLeaves),
    )

    private fun labels(state: WheelMenuState) = state.items.map { it.label }

    private fun navigator(): Pair<WheelMenuNavigator, WheelMenuState> {
        val state = WheelMenuState(root())
        val nav = WheelMenuNavigator(state, root())
        return nav to state
    }

    @Test
    fun `starts at root showing the top-level items`() {
        val (nav, state) = navigator()
        assertThat(nav.isAtRoot).isTrue()
        assertThat(nav.depth).isEqualTo(0)
        assertThat(labels(state)).isEqualTo(listOf("Change View", "Select Drone"))
    }

    @Test
    fun `drilling into a parent shows its children and syncs the state`() {
        val (nav, state) = navigator()
        assertThat(nav.drillInto(0)).isTrue()
        assertThat(nav.isAtRoot).isFalse()
        assertThat(nav.depth).isEqualTo(1)
        assertThat(labels(state)).isEqualTo(listOf("Chase", "FPV", "Top"))
    }

    @Test
    fun `drilling into a leaf is a no-op`() {
        val (nav, state) = navigator()
        nav.drillInto(0) // into Change View
        assertThat(nav.drillInto(0)).isFalse() // Chase is a leaf
        assertThat(nav.depth).isEqualTo(1)
        assertThat(labels(state)).isEqualTo(listOf("Chase", "FPV", "Top"))
    }

    @Test
    fun `drilling into an out-of-range index is a no-op`() {
        val (nav, _) = navigator()
        assertThat(nav.drillInto(9)).isFalse()
        assertThat(nav.isAtRoot).isTrue()
    }

    @Test
    fun `back pops one level and restores the parent items`() {
        val (nav, state) = navigator()
        nav.drillInto(1) // Select Drone
        assertThat(nav.back()).isTrue()
        assertThat(nav.isAtRoot).isTrue()
        assertThat(labels(state)).isEqualTo(listOf("Change View", "Select Drone"))
    }

    @Test
    fun `back at the root is a no-op`() {
        val (nav, _) = navigator()
        assertThat(nav.back()).isFalse()
        assertThat(nav.isAtRoot).isTrue()
    }

    @Test
    fun `reset returns to the root from any depth`() {
        val (nav, state) = navigator()
        nav.drillInto(0)
        nav.reset()
        assertThat(nav.isAtRoot).isTrue()
        assertThat(labels(state)).isEqualTo(listOf("Change View", "Select Drone"))
    }

    @Test
    fun `current exposes the deepest level`() {
        val (nav, _) = navigator()
        nav.drillInto(0)
        assertThat(nav.current.map { it.label }).isEqualTo(listOf("Chase", "FPV", "Top"))
    }

    @Test
    fun `swapping root while at root updates the shown items`() {
        val (nav, state) = navigator()
        nav.root = listOf(parent("Change View", viewLeaves))
        assertThat(labels(state)).isEqualTo(listOf("Change View"))
    }

    @Test
    fun `swapping root while drilled leaves the open submenu intact until reset`() {
        val (nav, state) = navigator()
        nav.drillInto(0)
        nav.root = listOf(parent("Only View", viewLeaves))
        // Still showing the submenu we drilled into.
        assertThat(labels(state)).isEqualTo(listOf("Chase", "FPV", "Top"))
        // The new root takes effect on the next reset (wheel close).
        nav.reset()
        assertThat(labels(state)).isEqualTo(listOf("Only View"))
    }
}
