package com.px4.hawkeye.android

import android.app.NativeActivity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.feature.replay.presentation.ReplayAction
import com.px4.hawkeye.feature.replay.presentation.ReplayRoot
import com.px4.hawkeye.feature.replay.presentation.ReplayViewModel
import org.koin.android.ext.android.getKoin

/**
 * The renderer host. C/raylib owns the GL surface; this class only adds a Compose
 * overlay for the "No file loaded" empty-state dialog.
 *
 * VIEW/SEND intents are NOT handled here — they go through [IntentRouterActivity], which
 * does the file ingest and then starts this activity with no intent data. That isolation
 * is what fixes the `DrawMesh+76` crash: cold-launching a NativeActivity with a VIEW
 * intent races Raylib's `InitGraphicsDevice` against the system's
 * `screenOrientation="landscape"` enforcement, leaving the EGL context in a state where
 * `glCreateShader`/`glGenTextures` return 0 and the materials end up with `shader.locs = NULL`.
 *
 * NativeActivity extends [android.app.Activity], not [androidx.activity.ComponentActivity],
 * so the lifecycle / ViewModelStore / SavedStateRegistry owners that Compose + Koin need
 * are implemented by hand — same call ordering ComponentActivity uses internally
 * (attach/restore before super.onCreate, ON_PAUSE/STOP/DESTROY before super,
 * ON_START/RESUME after super).
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

    // Resolved via Koin directly — `by viewModel()` from `koin-androidx` is restricted
    // to `ComponentActivity` / `Fragment`, and NativeActivity extends bare
    // `android.app.Activity`. Since `configChanges` in the manifest covers everything
    // we'd care about, the activity isn't recreated mid-session and the VM lifetime
    // matches the process — equivalent in practice to a ViewModelStore-scoped VM.
    private lateinit var viewModel: ReplayViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(savedInstanceState)
        super.onCreate(savedInstanceState)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        viewModel = getKoin().get<ReplayViewModel>()
        attachComposeOverlay()
        val fromFreshIngest = intent?.getBooleanExtra(EXTRA_FROM_TRAMPOLINE, false) == true
        viewModel.onAction(ReplayAction.OnAppStarted(fromFreshIngest = fromFreshIngest))
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
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        if (!isChangingConfigurations) viewModelStoreInstance.clear()
        super.onDestroy()
    }

    private fun attachComposeOverlay() {
        val composeView = ComposeView(this).apply { setBackgroundColor(Color.TRANSPARENT) }
        // Owners must be set before addContentView — the Compose recomposer binds to
        // ViewTreeLifecycleOwner at attach time.
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)

        composeView.setContent {
            HawkeyeTheme {
                ReplayRoot(viewModel = viewModel)
            }
        }

        window.addContentView(
            composeView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    companion object {
        /**
         * Set by `IntentRouterActivity` on the intent it uses to launch us, so we
         * know the inbox holds a file the user just explicitly opened (and we
         * shouldn't wipe it on cold-launch like we do for a plain icon tap).
         */
        const val EXTRA_FROM_TRAMPOLINE = "com.px4.hawkeye.android.from_trampoline"
    }
}
