package com.px4.hawkeye.feature.live.presentation

import com.px4.hawkeye.core.domain.DeviceIpProvider

class FakeDeviceIpProvider(private val ip: String?) : DeviceIpProvider {
    override fun localIpAddress(): String? = ip
}
