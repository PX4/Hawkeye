package com.px4.hawkeye.android.render

/**
 * Active-view snapshot published by the render thread (see `view_control.h`): the current
 * perspective camera mode, the current fullscreen ortho mode (0 = perspective), and whether
 * the 3-up ortho side panel is showing. The wheel uses it to mark the active slice.
 */
data class ViewSnapshot(
    val camMode: Int,
    val orthoMode: Int,
    val panelVisible: Boolean,
) {
    companion object {
        // Mirrors camera_mode_t in src/scene.h.
        const val CAM_CHASE = 0
        const val CAM_FPV = 1
        const val CAM_FREE = 2

        // Mirrors ortho_mode_t in src/scene.h (0 = perspective / no ortho).
        const val ORTHO_NONE = 0
        const val ORTHO_TOP = 1
        const val ORTHO_BOTTOM = 2
        const val ORTHO_FRONT = 3
        const val ORTHO_BACK = 4
        const val ORTHO_LEFT = 5
        const val ORTHO_RIGHT = 6

        /** Matches android_main.c's startup state (free-track) before the first publish. */
        val Default = ViewSnapshot(camMode = CAM_FREE, orthoMode = ORTHO_NONE, panelVisible = false)
    }
}

/**
 * Switches the renderer's camera/ortho view and the 3-up side panel. The implementation
 * talks to C via JNI; tests use a fake. Requests are marshalled to the render thread through
 * a lock-free control surface (see `view_control.h`).
 */
interface ViewController {
    fun view(): ViewSnapshot

    /** Selects a perspective camera ([ViewSnapshot.CAM_CHASE]/FPV/FREE); also exits ortho. */
    fun setCamMode(camMode: Int)

    /** Switches into a fullscreen ortho view ([ViewSnapshot.ORTHO_TOP]..ORTHO_RIGHT). */
    fun setOrthoMode(orthoMode: Int)

    /** Toggles the 3-up ortho side panel. */
    fun toggleSidePanel()

    /**
     * Px reserved at the top of the render surface for the Compose media bar, so the 3-up
     * panel's top view is not hidden behind it. Updated whenever the overlay's height changes.
     */
    fun setTopInset(px: Int)
}

/** JNI-backed [ViewController]. Only used inside the `:renderer` process. */
class NativeViewController : ViewController {

    override fun view(): ViewSnapshot {
        val s = nativeGetView() // [camMode, orthoMode, panelVisible]
        if (s.size < 3) return ViewSnapshot.Default
        return ViewSnapshot(camMode = s[0], orthoMode = s[1], panelVisible = s[2] != 0)
    }

    // camMode and orthoMode are mutually exclusive requests: the unused one is -1 (no-op).
    override fun setCamMode(camMode: Int) = nativeSetView(camMode, -1)
    override fun setOrthoMode(orthoMode: Int) = nativeSetView(-1, orthoMode)
    override fun toggleSidePanel() = nativeToggleSidePanel()
    override fun setTopInset(px: Int) = nativeSetTopInset(px)

    private external fun nativeSetView(camMode: Int, orthoMode: Int)
    private external fun nativeToggleSidePanel()
    private external fun nativeSetTopInset(px: Int)
    private external fun nativeGetView(): IntArray

    companion object {
        init {
            // Already loaded by native_app_glue when the NativeActivity starts; idempotent.
            System.loadLibrary("hawkeye")
        }
    }
}
