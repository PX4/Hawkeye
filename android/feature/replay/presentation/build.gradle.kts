plugins {
    id("hawkeye.android.feature")
}

android {
    namespace = "com.px4.hawkeye.feature.replay.presentation"
    resourcePrefix = "replay_"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:presentation"))
    implementation(project(":core:design-system"))
    implementation(project(":core:navigation"))
}
