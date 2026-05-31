package com.px4.hawkeye.android

import android.app.NativeActivity
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.px4.hawkeye.android.render.NativeReplayController
import com.px4.hawkeye.android.render.transport.TransportRoot
import com.px4.hawkeye.android.render.transport.TransportViewModel
import com.px4.hawkeye.core.designsystem.HawkeyeTheme

/**
 * The renderer host. C/raylib owns the GL surface; this class adds a Compose overlay for
 * the touch transport controls (play/pause, scrub, speed), which drive the native engine
 * through [NativeReplayController]'s JNI surface.
 *
 * Because raylib owns the whole NativeActivity window surface, an inline overlay would be
 * clobbered by GL buffer swaps. Instead the overlay lives in a dedicated full-width
 * `WindowManager` panel window layered above the renderer (see [attachTransportOverlay]).
 * Creating that window ourselves lets us set `layoutInDisplayCutoutMode = ALWAYS` and
 * `MATCH_PARENT` width up front, so the bar spans edge to edge under the cutout — which a
 * Compose `Popup` window cannot do.
 *
 * Runs in its own `:renderer` process (manifest) and hard-exits on teardown ([onDestroy]):
 * raylib's Android render loop parks in `ALooper_pollAll` once the window is torn down and
 * never returns, so a graceful destroy would block the process main thread. Halting keeps
 * the Compose shell responsive and guarantees a clean raylib cold start per replay; it also
 * tears down the panel window with the process.
 *
 * `NativeActivity` extends bare `android.app.Activity`, so the Lifecycle / ViewModelStore /
 * SavedStateRegistry owners that Compose needs are implemented by hand. Koin is not started
 * in `:renderer`, so the ViewModel is built with a plain [ViewModelProvider.Factory].
 */
class HawkeyeActivity :
    NativeActivity(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val viewModelStoreInstance = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = viewModelStoreInstance

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val transportViewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TransportViewModel(NativeReplayController()) as T
    }

    private lateinit var viewModel: TransportViewModel
    private var transportOverlay: ComposeView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(savedInstanceState)
        super.onCreate(savedInstanceState)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        goEdgeToEdge()
        viewModel = ViewModelProvider(this, transportViewModelFactory)[TransportViewModel::class.java]
    }

    /** Draw under the system bars + cutout and hide the bars for a clean fullscreen replay. */
    private fun goEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // raylib/NativeActivity resets system UI on focus changes; re-assert immersive.
            hideSystemBars()
            // The window is attached and has a valid token by now, so the panel can be added.
            attachTransportOverlay()
        }
    }

    /**
     * Adds the transport overlay as a full-width sub-window above the renderer. Full width +
     * cutout-always (set at creation) make the bar reach both edges; NOT_FOCUSABLE leaves
     * Back to the renderer, NOT_TOUCH_MODAL + a wrap-height top window let camera gestures
     * below the bar fall through to the GL surface.
     */
    private fun attachTransportOverlay() {
        if (transportOverlay != null) return
        val composeView = ComposeView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setViewTreeLifecycleOwner(this@HawkeyeActivity)
            setViewTreeViewModelStoreOwner(this@HawkeyeActivity)
            setViewTreeSavedStateRegistryOwner(this@HawkeyeActivity)
            setContent {
                HawkeyeTheme {
                    TransportRoot(viewModel = viewModel)
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            token = window.decorView.windowToken
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
        windowManager.addView(composeView, params)
        transportOverlay = composeView
    }

    override fun onStart() {
        super.onStart()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onResume() {
        super.onResume()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onPause() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        super.onPause()
    }

    override fun onStop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        savedStateRegistryController.performSave(outState)
    }

    override fun onDestroy() {
        // Hard-exit the dedicated renderer process rather than block the main thread joining
        // the non-terminating native render loop. Halting skips graceful teardown; the OS
        // reclaims the process (and the panel window). Preserves the Plan 2a ANR/crash fix.
        Runtime.getRuntime().halt(0)
    }
}
