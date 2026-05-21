plugins {
    id("hawkeye.android.library.compose")
}

android {
    namespace = "com.px4.hawkeye.core.presentation"
    resourcePrefix = "core_presentation_"
}

dependencies {
    api(project(":core:domain"))
}
