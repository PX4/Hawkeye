package com.px4.hawkeye.feature.live.data.di

import com.px4.hawkeye.core.domain.DeviceIpProvider
import com.px4.hawkeye.feature.live.data.AndroidDeviceIpProvider
import org.koin.dsl.module

val liveDataModule = module {
    single<DeviceIpProvider> { AndroidDeviceIpProvider() }
}
