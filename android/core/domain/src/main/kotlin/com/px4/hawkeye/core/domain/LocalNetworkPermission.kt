package com.px4.hawkeye.core.domain

/**
 * Answers whether the app may use the device's local network, which a live MAVLink session
 * needs in both directions. Sits next to [DeviceIpProvider] because it is the same kind of
 * thing: a platform question the live setup screen has to ask before it can do its job.
 *
 * "May" rather than "was granted": on the Android versions that do not police local network
 * access there is no permission to hold, and the honest answer is still yes.
 */
fun interface LocalNetworkPermission {
    fun isGranted(): Boolean
}
