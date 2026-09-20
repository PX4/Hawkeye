package com.px4.hawkeye.feature.live.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.px4.hawkeye.core.domain.LocalNetworkPermission

/**
 * Platform answer to [LocalNetworkPermission].
 *
 * Until Android 17 the INTERNET permission implicitly covered the device's own LAN, so the
 * renderer could bind its MAVLink UDP socket with nothing asked of the user. Apps targeting
 * API 37 lose that: unicast, multicast and broadcast on the local network all fail with
 * EPERM until [Manifest.permission.ACCESS_LOCAL_NETWORK] is granted. The enforcement lives
 * in the networking stack, so it reaches the renderer's native BSD sockets exactly as it
 * reaches anything in Kotlin, and a grant is per UID, so obtaining it in the main process
 * also covers the socket the `:renderer` process opens.
 *
 * Runtime grants are re-read on every call rather than cached, because Android revokes
 * permissions for unused apps and a cached yes would outlive the grant.
 */
class AndroidLocalNetworkPermission(
    private val context: Context,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
) : LocalNetworkPermission {

    // Context.checkSelfPermission rather than ContextCompat: it exists since API 23, well
    // below this module's minSdk, so the androidx dependency would buy nothing here.
    override fun isGranted(): Boolean = !isEnforced(sdkInt) ||
        context.checkSelfPermission(NAME) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val NAME: String = Manifest.permission.ACCESS_LOCAL_NETWORK

        /**
         * Whether this device polices local network access at all. Keyed on the device, not
         * on our target: below API 37 the platform does not define the permission, so
         * `checkSelfPermission` would answer DENIED forever and asking would strand Live on
         * every older device we support. Kept separate and pure so that rule is testable
         * without a device, which matters because it is the one line here that could break
         * the entire pre-37 install base and no emulator we run can reach the false branch.
         */
        internal fun isEnforced(sdkInt: Int): Boolean =
            sdkInt >= Build.VERSION_CODES.CINNAMON_BUN
    }
}
