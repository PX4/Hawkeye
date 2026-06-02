package com.px4.hawkeye.core.domain

/**
 * Supplies the device's local (LAN/Wi-Fi) IPv4 address so the UI can tell the user where
 * to point a MAVLink stream. Returns null when no suitable address is available.
 */
fun interface DeviceIpProvider {
    fun localIpAddress(): String?
}
