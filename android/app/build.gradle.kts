plugins {
    id("hawkeye.android.application")
}

// Release builds take their version from the git tag, passed as
// -PhawkeyeVersionName=<x.y.z> by .github/workflows/release.yml. Local and CI debug
// builds fall back to a dev version so no extra flags are needed.
val hawkeyeVersionName: String =
    providers.gradleProperty("hawkeyeVersionName").getOrElse("0.0.0-dev")

// Monotonic code derived from the semver: 0.4.0 -> 4000, 1.2.3 -> 1002003.
//
// Parsed strictly. A malformed version has to fail the build rather than degrade to a
// low code, because Android refuses any upgrade whose version code is not greater than
// the installed one, and a silently-wrong code would ship in a release.
val hawkeyeVersionCode: Int = run {
    val parts = hawkeyeVersionName.substringBefore('-').split('.')
    require(parts.size == 3) {
        "hawkeyeVersionName must be MAJOR.MINOR.PATCH with an optional -suffix, got '$hawkeyeVersionName'"
    }
    val (major, minor, patch) = parts.map { part ->
        part.toIntOrNull()?.takeIf { it in 0..999 }
            ?: error("hawkeyeVersionName component '$part' is not an integer in 0..999, from '$hawkeyeVersionName'")
    }
    // The 1000 radix leaves room for two-digit and three-digit minor/patch numbers.
    // 0.0.0 is only reachable via the dev fallback above, which needs a floor of 1.
    (major * 1_000_000 + minor * 1_000 + patch).coerceAtLeast(1)
}

android {
    namespace = "com.px4.hawkeye.android"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    ndkVersion = "30.0.14904198"

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    defaultConfig {
        applicationId = "com.px4.hawkeye.android"
        targetSdk = 36
        versionCode = hawkeyeVersionCode
        versionName = hawkeyeVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                // Changing this list means updating android/scripts/verify-release-apk.sh
                // and the ABI list documented in docs/installation.md,
                // docs/developer/releasing.md, docs/troubleshooting.md, and README.md.
                abiFilters += listOf("arm64-v8a", "x86_64")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    testOptions { unitTests.all { it.useJUnitPlatform() } }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:presentation"))
    implementation(project(":core:design-system"))
    implementation(project(":feature:replay:data"))
    implementation(project(":feature:replay:presentation"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:home:presentation"))
    implementation(project(":feature:settings:domain"))
    implementation(project(":feature:settings:data"))
    implementation(project(":feature:settings:presentation"))
    implementation(project(":feature:live:domain"))
    implementation(project(":feature:live:data"))
    implementation(project(":feature:live:presentation"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.savedstate.ktx)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // Pin window-core to the version the app runs on so the layout test can use the
    // modern WindowSizeClass(Int, Int) constructor (the adaptive lib only pulls 1.3.0).
    androidTestImplementation(libs.androidx.window)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
