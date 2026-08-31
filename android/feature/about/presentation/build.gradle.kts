plugins {
    id("hawkeye.android.feature")
}

android {
    namespace = "com.px4.hawkeye.feature.about.presentation"
    resourcePrefix = "about_"
}

dependencies {
    implementation(project(":core:presentation"))
    implementation(project(":core:design-system"))

    implementation(libs.androidx.lifecycle.runtime.compose)
}
