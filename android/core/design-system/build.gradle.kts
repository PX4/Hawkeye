plugins {
    id("hawkeye.android.library.compose")
}

android {
    namespace = "com.px4.hawkeye.core.designsystem"
    resourcePrefix = "core_ds_"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    // Espresso 3.5.0 (the transitive default) breaks input injection on Android 14+;
    // pin the catalog version, same as :app.
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
