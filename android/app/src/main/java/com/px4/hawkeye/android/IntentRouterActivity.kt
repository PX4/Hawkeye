package com.px4.hawkeye.android

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.core.presentation.ObserveAsEvents
import com.px4.hawkeye.core.presentation.asString
import com.px4.hawkeye.feature.replay.presentation.ReplayAction
import com.px4.hawkeye.feature.replay.presentation.ReplayEvent
import com.px4.hawkeye.feature.replay.presentation.ReplayScreen
import com.px4.hawkeye.feature.replay.presentation.ReplayViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Trampoline activity that owns every inbound VIEW/SEND intent. Renders the "Open ULog?"
 * confirm dialog over the current foreground task, ingests the file via
 * [com.px4.hawkeye.feature.replay.data.AndroidUlogInboxDataSource], then hands off to
 * [HawkeyeActivity] with a plain `startActivity` (no intent action/data).
 *
 * Why this exists: cold-launching `NativeActivity` with a VIEW/SEND intent races Raylib's
 * `InitGraphicsDevice` against the system's orientation/surface setup — `glCreateShader`
 * returns 0, the default shader/texture fail to load, and the first `DrawMesh` segfaults
 * on `material.shader.locs[12]`. By routing intents through a plain `ComponentActivity`,
 * `HawkeyeActivity` is only ever launched the boring way (no action, no data), which the
 * logs confirm always succeeds.
 */
class IntentRouterActivity : ComponentActivity() {

    private val viewModel: ReplayViewModel by viewModel()

    /**
     * True after the user has tapped Open. We only start [HawkeyeActivity] when the
     * subsequent `ShowToast` event arrives — distinguishing the ingest-finished case
     * from a preview-failure toast that should just close the trampoline.
     */
    private var userConfirmed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = extractUri(intent)?.toString()
        if (uri == null) {
            // No URI to act on — just send the user to the renderer with the same
            // "no fresh ingest" semantics as a launcher tap (inbox gets wiped).
            startActivity(Intent(this, HawkeyeActivity::class.java))
            finish()
            return
        }

        viewModel.onAction(ReplayAction.OnIntentReceived(uri))

        setContent {
            HawkeyeTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val context = LocalContext.current

                ObserveAsEvents(viewModel.events) { event ->
                    when (event) {
                        is ReplayEvent.ShowToast -> {
                            Toast.makeText(
                                context,
                                event.text.asString(context),
                                Toast.LENGTH_SHORT
                            ).show()
                            if (userConfirmed) {
                                startActivity(
                                    Intent(this@IntentRouterActivity, HawkeyeActivity::class.java)
                                        .putExtra(HawkeyeActivity.EXTRA_FROM_TRAMPOLINE, true)
                                )
                            }
                            finish()
                        }
                    }
                }

                ReplayScreen(state = state, onAction = ::handleAction)
            }
        }
    }

    /**
     * With `launchMode="singleInstance"` a second share while the dialog is still up
     * is delivered here rather than to a new activity instance. Forward the new URI to
     * the VM (whose [com.px4.hawkeye.feature.replay.presentation.ReplayViewModel.onAction]
     * already cancels the in-flight `previewJob` and replaces the `ConfirmOpen` state),
     * and reset [userConfirmed] so a stale tap on the previous file's Open button can't
     * leak into the new flow.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val uri = extractUri(intent)?.toString()
        if (uri == null) {
            startActivity(Intent(this, HawkeyeActivity::class.java))
            finish()
            return
        }
        userConfirmed = false
        viewModel.onAction(ReplayAction.OnIntentReceived(uri))
    }

    private fun handleAction(action: ReplayAction) {
        when (action) {
            ReplayAction.OnConfirmOpen -> {
                userConfirmed = true
                viewModel.onAction(action)
            }
            ReplayAction.OnDismissDialog -> {
                viewModel.onAction(action)
                finish()
            }
            else -> viewModel.onAction(action)
        }
    }

    private fun extractUri(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> getStreamExtra(intent)
            else -> null
        }
    }

    private fun getStreamExtra(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
}
