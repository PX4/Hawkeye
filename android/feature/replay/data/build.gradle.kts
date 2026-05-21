plugins {
    id("hawkeye.android.library")
}

android {
    namespace = "com.px4.hawkeye.feature.replay.data"
}

dependencies {
    implementation(project(":core:domain"))
    api(project(":feature:replay:domain"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
}
