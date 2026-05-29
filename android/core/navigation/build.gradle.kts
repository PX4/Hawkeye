plugins {
    id("hawkeye.android.library.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.px4.hawkeye.core.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.navigation3.ui)
    api(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.koin.android)
}
