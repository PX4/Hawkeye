package com.px4.hawkeye.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureAndroidCompose(extension: CommonExtension) {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

    extension.buildFeatures.compose = true

    val composeBom = libs.lib("androidx-compose-bom")

    dependencies {
        add("implementation", platform(composeBom))
        add("androidTestImplementation", platform(composeBom))

        add("implementation", libs.lib("androidx-compose-ui"))
        add("implementation", libs.lib("androidx-compose-ui-tooling-preview"))
        add("implementation", libs.lib("androidx-compose-material3"))
        add("implementation", libs.lib("androidx-compose-material-icons-core"))
        add("implementation", libs.lib("androidx-activity-compose"))
        add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))

        add("debugImplementation", libs.lib("androidx-compose-ui-tooling"))
    }
}
