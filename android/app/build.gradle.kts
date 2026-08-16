plugins {
    id("hawkeye.android.application")
}

// Release builds take their version from the git tag, passed as
// -PhawkeyeVersionName=<x.y.z> by .github/workflows/release.yml. Local and CI debug
// builds fall back to a dev version so no extra flags are needed.
val hawkeyeVersionName: String =
    providers.gradleProperty("hawkeyeVersionName").getOrElse("0.0.0-dev")

// Monotonic code derived from the semver: 0.4.0-rc1 -> 400001, 0.4.0 -> 400099,
// 1.2.3 -> 100200399. The rc component orders every prerelease below its final
// release, so both can ship to Google Play, which refuses a version code it has
// already seen.
//
// Parsed strictly. A malformed version has to fail the build rather than degrade to a
// low code, because Android refuses any upgrade whose version code is not greater than
// the installed one, and a silently-wrong code would ship in a release.
val hawkeyeVersionCode: Int = run {
    val base = hawkeyeVersionName.substringBefore('-')
    val suffix = hawkeyeVersionName.substringAfter('-', missingDelimiterValue = "")
    val parts = base.split('.')
    require(parts.size == 3) {
        "hawkeyeVersionName must be MAJOR.MINOR.PATCH with an optional -suffix, got '$hawkeyeVersionName'"
    }
    val (major, minor, patch) = parts.map { part ->
        part.toIntOrNull()?.takeIf { it in 0..999 }
            ?: error("hawkeyeVersionName component '$part' is not an integer in 0..999, from '$hawkeyeVersionName'")
    }
    // The 100_000_000 radix overflows Google Play's version code cap of 2,100,000,000
    // once major exceeds 20.
    require(major <= 20) {
        "hawkeyeVersionName major '$major' pushes the version code past Google Play's cap, from '$hawkeyeVersionName'"
    }
    // A final release takes 99 so it sorts above every rc of the same version; dev and
    // ci builds take 0 so they sort below both. The empty-suffix branch also requires
    // the absence of a '-', because a trailing-hyphen name like "0.4.0-" would otherwise
    // silently take the final release's code and burn it on Google Play.
    val rc = when {
        suffix.isEmpty() && '-' !in hawkeyeVersionName -> 99
        suffix == "dev" || suffix == "ci" -> 0
        suffix.matches(Regex("rc[1-9][0-9]?")) ->
            suffix.removePrefix("rc").toInt().also {
                require(it <= 98) { "rc99 collides with the final release's version code, from '$hawkeyeVersionName'" }
            }
        else -> error("hawkeyeVersionName suffix '$suffix' is not rc1..rc98, dev, or ci, from '$hawkeyeVersionName'")
    }
    // 0.0.0-dev (the local fallback above) and CI's 0.0.0-ci both compute to 0, so the
    // floor of 1 covers them both.
    (major * 100_000_000 + minor * 100_000 + patch * 100 + rc).coerceAtLeast(1)
}

// Release signing activates only when the environment provides an upload keystore
// (HAWKEYE_UPLOAD_KEYSTORE is a path to a decoded .jks). CI injects it from repository
// secrets; local and PR builds have none and keep producing an unsigned release APK.
// Blank counts as absent because a workflow env mapping of an unset secret yields an
// empty string, and that case has to stay on the unsigned path.
val uploadKeystorePath: String? = providers.environmentVariable("HAWKEYE_UPLOAD_KEYSTORE")
    .orNull?.takeIf { it.isNotBlank() }

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
                // Changing this list means updating android/scripts/verify-release-apk.sh,
                // android/scripts/verify-release-bundle.sh, and the ABI list documented
                // in docs/installation.md, docs/developer/releasing.md,
                // docs/troubleshooting.md, and README.md.
                abiFilters += listOf("arm64-v8a", "x86_64")
            }
        }
    }

    signingConfigs {
        if (uploadKeystorePath != null) {
            val password = providers.environmentVariable("HAWKEYE_UPLOAD_KEYSTORE_PASSWORD").orNull
            val alias = providers.environmentVariable("HAWKEYE_UPLOAD_KEY_ALIAS").orNull
            // Checked here rather than left to .get() so a half-configured environment
            // fails at configuration time with the trio contract spelled out, not deep
            // in :app:packageRelease. isNullOrEmpty, not isPresent: an env mapping of an
            // unset secret arrives as an empty string.
            require(!password.isNullOrEmpty() && !alias.isNullOrEmpty()) {
                "HAWKEYE_UPLOAD_KEYSTORE is set, so HAWKEYE_UPLOAD_KEYSTORE_PASSWORD and " +
                    "HAWKEYE_UPLOAD_KEY_ALIAS must be set too"
            }
            create("upload") {
                storeFile = file(uploadKeystorePath)
                storePassword = password
                keyAlias = alias
                // The upload keystore is PKCS12, which has a single password.
                keyPassword = password
            }
        }
    }

    buildTypes {
        release {
            if (uploadKeystorePath != null) {
                signingConfig = signingConfigs.getByName("upload")
            }
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
