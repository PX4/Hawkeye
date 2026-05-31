plugins {
    id("hawkeye.android.feature")
}

android {
    namespace = "com.px4.hawkeye.feature.home.presentation"
    resourcePrefix = "home_"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:presentation"))
    implementation(project(":core:design-system"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.window)
}
