plugins {
    id("hawkeye.android.library")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.px4.hawkeye.feature.replay.data"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(project(":core:domain"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
}
