package com.px4.hawkeye.feature.live.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Gate for the local-network access a live session needs.
 *
 * Until Android 17 the INTERNET permission implicitly covered the device's own LAN, so the
 * renderer could bind its MAVLink UDP socket with nothing asked of the user. Apps targeting
 * API 37 lose that: unicast, multicast and broadcast on the local network all fail with
 * EPERM until [Manifest.permission.ACCESS_LOCAL_NETWORK] is granted. The enforcement lives
 * in the networking stack, so it reaches the renderer's native BSD sockets exactly as it
 * reaches anything in Kotlin.
 *
 * The check is keyed on the device, not on our target: on API 36 and below the platform
 * does not define the permission at all, `checkSelfPermission` would answer DENIED forever,
 * and asking for it would strand Live on every older device.
 *
 * Runtime grants are per UID, so a grant obtained here also covers the `:renderer` process
 * that actually opens the socket.
 */
internal object LocalNetworkPermission {

    const val NAME: String = Manifest.permission.ACCESS_LOCAL_NETWORK

    /** True only where the platform enforces the permission (Android 17, API 37, and up). */
    private val isEnforced: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN

    fun isGranted(context: Context): Boolean = !isEnforced ||
        ContextCompat.checkSelfPermission(context, NAME) == PackageManager.PERMISSION_GRANTED
}
