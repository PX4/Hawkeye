package com.px4.hawkeye.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project

internal const val PROJECT_COMPILE_SDK = 36
// Raising this means updating the "Android 10 (API 29)" claim in docs/installation.md,
// docs/developer/releasing.md, and README.md.
internal const val PROJECT_MIN_SDK = 29
internal const val PROJECT_TARGET_SDK = 36

internal fun Project.configureAndroidCommon(extension: CommonExtension) {
    extension.compileSdk = PROJECT_COMPILE_SDK
    extension.defaultConfig.minSdk = PROJECT_MIN_SDK
    extension.compileOptions.sourceCompatibility = JavaVersion.VERSION_11
    extension.compileOptions.targetCompatibility = JavaVersion.VERSION_11
}
