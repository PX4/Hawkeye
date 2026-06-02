plugins {
    id("hawkeye.android.feature")
}

android {
    namespace = "com.px4.hawkeye.feature.live.presentation"
    resourcePrefix = "live_"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:presentation"))
    implementation(project(":core:design-system"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:live:domain"))
}
