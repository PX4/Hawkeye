plugins {
    id("hawkeye.jvm.library")
}

dependencies {
    // Flow appears in ReplayLibraryRepository's public API, so expose it transitively.
    api(libs.kotlinx.coroutines.core)
}
