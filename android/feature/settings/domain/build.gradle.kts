plugins {
    id("hawkeye.jvm.library")
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.kotlinx.coroutines.core)
}
