plugins {
    id("hawkeye.android.library")
}

android {
    namespace = "com.px4.hawkeye.feature.live.data"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":feature:live:domain"))
    implementation(libs.koin.android)
}
