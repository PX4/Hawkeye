package com.px4.hawkeye.feature.live.data

import com.px4.hawkeye.core.domain.DeviceIpProvider
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Resolves the device's LAN IPv4 via [NetworkInterface] (permission-free, unlike the
 * deprecated Wi-Fi-only WifiManager path). Returns the first non-loopback, up, site-local
 * IPv4 address, or null when none is available (e.g. no network).
 */
class AndroidDeviceIpProvider : DeviceIpProvider {
    override fun localIpAddress(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }
            ?.hostAddress
    }.getOrNull()
}
