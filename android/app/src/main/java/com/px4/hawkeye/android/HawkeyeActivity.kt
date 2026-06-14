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
import com.px4.hawkeye.android.render.NativeLiveStatusController
import com.px4.hawkeye.android.render.NativeReplayController
import com.px4.hawkeye.android.render.NativeSwarmController
import com.px4.hawkeye.android.render.RenderMode
import com.px4.hawkeye.android.render.RendererLauncher
import com.px4.hawkeye.android.render.live.LiveStatusRoot
import com.px4.hawkeye.android.render.live.LiveStatusViewModel
import com.px4.hawkeye.android.render.swarm.SwarmWheelRoot
import com.px4.hawkeye.android.render.swarm.SwarmWheelViewModel
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

    private val liveStatusViewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LiveStatusViewModel(NativeLiveStatusController(), deviceIp) as T
    }

    private val swarmWheelViewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SwarmWheelViewModel(NativeSwarmController(), droneLabels) as T
    }

    private var overlay: ComposeView? = null
    private var wheelOverlay: ComposeView? = null

    // Read once from the launch Intent. Safe to cache: the activity uses the default
    // (standard) launch mode, so each launch is a brand-new instance with a fresh Intent;
    // onDestroy then halts the :renderer process. There is no in-place reuse (no
    // singleTop/onNewIntent path) that could leave these values stale.
    private val isLiveMode: Boolean by lazy {
        intent?.getStringExtra(RendererLauncher.EXTRA_MODE) == RenderMode.LIVE.name
    }

    // Device LAN IP, resolved by the launcher and passed in so the live overlay can show the
    // user where to point PX4 (the :renderer process has no Koin to inject a provider).
    private val deviceIp: String? by lazy {
        intent?.getStringExtra(RendererLauncher.EXTRA_DEVICE_IP)
    }

    // Staged logs' display names in drone order, for the swarm wheel's slice labels. The
    // ViewModel falls back to numbered names when a label is missing or blank.
    private val droneLabels: List<String> by lazy {
        intent?.getStringArrayExtra(RendererLauncher.EXTRA_DRONE_LABELS)?.toList().orEmpty()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(savedInstanceState)
        super.onCreate(savedInstanceState)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        goEdgeToEdge()
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
            // The window is attached and has a valid token by now, so the panels can be added.
            attachOverlay()
            attachWheelOverlay()
        }
    }

    /**
     * Adds the overlay as a full-width sub-window above the renderer: the replay transport bar
     * for a replay session, or the live status strip for a live session. Full width +
     * cutout-always (set at creation) make it reach both edges; NOT_FOCUSABLE leaves Back to
     * the renderer, NOT_TOUCH_MODAL + a wrap-height top window let camera gestures below it
     * fall through to the GL surface.
     */
    private fun attachOverlay() {
        if (overlay != null) return
        val composeView = ComposeView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setViewTreeLifecycleOwner(this@HawkeyeActivity)
            setViewTreeViewModelStoreOwner(this@HawkeyeActivity)
            setViewTreeSavedStateRegistryOwner(this@HawkeyeActivity)
            setContent {
                HawkeyeTheme {
                    if (isLiveMode) {
                        LiveStatusRoot(
                            viewModel = ViewModelProvider(
                                this@HawkeyeActivity, liveStatusViewModelFactory,
                            )[LiveStatusViewModel::class.java],
                        )
                    } else {
                        TransportRoot(
                            viewModel = ViewModelProvider(
                                this@HawkeyeActivity, transportViewModelFactory,
                            )[TransportViewModel::class.java],
                        )
                    }
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            // Edge-to-edge: NO_LIMITS + cutout-mode ALWAYS (set below) span the bar the full
            // display width, under the cutout. NOT_FOCUSABLE leaves Back to the renderer;
            // NOT_TOUCH_MODAL lets camera gestures below the bar reach raylib.
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
        overlay = composeView
    }

    /**
     * Adds the swarm drone-selection wheel as a second, full-screen panel above the renderer
     * (and above the transport bar — added later, so it z-orders on top; it only draws while
     * a hold gesture is in progress). Replay sessions only.
     *
     * The transport panel and this one split the overlay duties by touch policy, which is
     * window-global: the transport bar must RECEIVE touches (its wrap-height strip passes
     * the rest through by geometry), while the wheel surface must pass EVERY touch through
     * to the GL surface — the native engine owns the tap-and-hold detection — hence
     * FLAG_NOT_TOUCHABLE and MATCH_PARENT height here.
     */
    private fun attachWheelOverlay() {
        if (wheelOverlay != null || isLiveMode) return
        val composeView = ComposeView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setViewTreeLifecycleOwner(this@HawkeyeActivity)
            setViewTreeViewModelStoreOwner(this@HawkeyeActivity)
            setViewTreeSavedStateRegistryOwner(this@HawkeyeActivity)
            setContent {
                HawkeyeTheme {
                    SwarmWheelRoot(
                        viewModel = ViewModelProvider(
                            this@HawkeyeActivity, swarmWheelViewModelFactory,
                        )[SwarmWheelViewModel::class.java],
                    )
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            // NOT_TOUCHABLE routes every MotionEvent to the windows below (the GL surface);
            // NOT_FOCUSABLE leaves Back with the renderer; NO_LIMITS + cutout ALWAYS (below)
            // give the overlay the same full-display pixel space as the GL surface, so the
            // native gesture coordinates and the wheel drawing coordinates line up 1:1.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
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
        wheelOverlay = composeView
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
