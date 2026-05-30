package com.px4.hawkeye.feature.home.presentation

import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Full-bleed, muted, looping video rendered behind the Home content. Builds a
 * lifecycle-aware [ExoPlayer] (paused while the app is stopped, released on
 * dispose) and shows it through a texture-backed [PlayerView] so the Compose
 * scrim and UI layer cleanly on top.
 *
 * Under inspection (Compose previews) the player is skipped and a solid surface
 * is drawn instead, so the stateless screen stays previewable without a Context.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun HomeVideoBackground(modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) {
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // android.resource://<app package>/<resId>. context.packageName is the app
            // package, which is correct here even though R belongs to this library module.
            val uri = Uri.parse(
                "android.resource://${context.packageName}/${R.raw.home_background_video}",
            )
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> exoPlayer.play()
                Lifecycle.Event.ON_STOP -> exoPlayer.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            (LayoutInflater.from(ctx).inflate(R.layout.home_video_player, null) as PlayerView)
                .apply { player = exoPlayer }
        },
    )
}
