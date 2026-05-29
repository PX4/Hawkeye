plugins {
    id("hawkeye.android.library")
}

android {
    namespace = "com.px4.hawkeye.feature.settings.data"
}

dependencies {
    implementation(project(":feature:settings:domain"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
}
