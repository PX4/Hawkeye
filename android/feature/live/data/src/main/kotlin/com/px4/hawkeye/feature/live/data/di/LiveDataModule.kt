package com.px4.hawkeye.feature.live.data.di

import com.px4.hawkeye.core.domain.DeviceIpProvider
import com.px4.hawkeye.core.domain.LocalNetworkPermission
import com.px4.hawkeye.feature.live.data.AndroidDeviceIpProvider
import com.px4.hawkeye.feature.live.data.AndroidLocalNetworkPermission
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val liveDataModule = module {
    single<DeviceIpProvider> { AndroidDeviceIpProvider() }
    single<LocalNetworkPermission> { AndroidLocalNetworkPermission(androidContext()) }
}
