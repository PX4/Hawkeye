package com.px4.hawkeye.core.designsystem.wheel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

/**
 * Drives a [WheelMenuState] through a tree of [WheelMenuItem]s, turning the flat widget
 * into a nested menu. The navigator owns the level stack; [WheelMenuState] still owns "is
 * the wheel open / where is the finger". On every transition it assigns
 * [WheelMenuState.items] for the new level, reusing the widget's submenu-swap seam (which
 * clears a stale hover while open so a release can never report an index from another level).
 *
 * Timing lives in the gesture producer, not here: the producer decides when a dwell over a
 * parent should [drillInto] it and when a dwell in the hub should go [back]. The navigator
 * is pure and synchronous, so it unit-tests on the JVM with no composition.
 */
@Stable
class WheelMenuNavigator(
    private val state: WheelMenuState,
    initialRoot: List<WheelMenuItem>,
) {
    private val stack = ArrayDeque<List<WheelMenuItem>>()
    private var rootItems: List<WheelMenuItem> = initialRoot

    init {
        state.items = initialRoot
    }

    /**
     * The top-level menu. Swapping it (e.g. when the drone count or active view changes)
     * re-seeds the displayed level only while [isAtRoot], so a swap never yanks the user out
     * of an open submenu; the next [reset] picks up the new root regardless.
     */
    var root: List<WheelMenuItem>
        get() = rootItems
        set(value) {
            rootItems = value
            if (isAtRoot) state.items = value
        }

    /** The level currently shown: the deepest pushed submenu, or [root] at depth 0. */
    val current: List<WheelMenuItem>
        get() = stack.lastOrNull() ?: rootItems

    /** True when no submenu is open. */
    val isAtRoot: Boolean
        get() = stack.isEmpty()

    /** Number of submenu levels below the root (0 at the root). */
    val depth: Int
        get() = stack.size

    /**
     * Pushes the children of `current[index]` and shows them. No-op (returns false) when the
     * index is out of range or the item is a leaf (no [WheelMenuItem.children]).
     */
    fun drillInto(index: Int): Boolean {
        val children = current.getOrNull(index)?.children ?: return false
        stack.addLast(children)
        state.items = children
        return true
    }

    /**
     * Pops one level and restores the parent's items. No-op (returns false) at the root.
     *
     * Intentionally not wired by the root wheel: stepping back by returning the finger to
     * the hub didn't feel natural in use, so the app dropped that gesture (a submenu is now
     * left by selecting a leaf, or by releasing in the hub to cancel the whole gesture).
     * Kept here as a sensible, tested capability for any other host that does want a back.
     */
    fun back(): Boolean {
        if (stack.isEmpty()) return false
        stack.removeLast()
        state.items = current
        return true
    }

    /** Returns to the root level. Call when the wheel closes so the next gesture starts fresh. */
    fun reset() {
        stack.clear()
        state.items = rootItems
    }
}

/**
 * Remembers a [WheelMenuNavigator] bound to [state], seeded with [initialRoot] (read only at
 * first composition). Swap the root later through [WheelMenuNavigator.root] as the menu
 * changes, mirroring how [rememberWheelMenuState] seeds [WheelMenuState.items].
 */
@Composable
fun rememberWheelMenuNavigator(
    state: WheelMenuState,
    initialRoot: List<WheelMenuItem>,
): WheelMenuNavigator = remember(state) { WheelMenuNavigator(state, initialRoot) }
