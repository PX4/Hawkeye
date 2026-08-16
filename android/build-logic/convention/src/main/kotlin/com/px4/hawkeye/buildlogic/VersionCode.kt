package com.px4.hawkeye.buildlogic

/**
 * Monotonic Android versionCode derived from the semver versionName:
 * 0.4.0-rc1 -> 400001, 0.4.0 -> 400099, 1.2.3 -> 100200399.
 *
 * The rc component orders every prerelease below its final release, so both can ship to
 * Google Play, which refuses a version code it has already seen. A final release takes
 * 99 so it sorts above every rc of the same version; dev and ci builds take 0 so they
 * sort below both.
 *
 * Parsed strictly. A malformed version has to fail the build rather than degrade to a
 * low code, because Android refuses any upgrade whose version code is not greater than
 * the installed one, and a silently-wrong code would ship in a release. That includes a
 * trailing-hyphen name like "0.4.0-", which must not silently take the final release's
 * code and burn it on Google Play. VersionCodeTest holds the boundary table.
 */
fun hawkeyeVersionCode(versionName: String): Int {
    val base = versionName.substringBefore('-')
    val suffix = versionName.substringAfter('-', missingDelimiterValue = "")
    val parts = base.split('.')
    require(parts.size == 3) {
        "hawkeyeVersionName must be MAJOR.MINOR.PATCH with an optional -suffix, got '$versionName'"
    }
    val (major, minor, patch) = parts.map { part ->
        part.toIntOrNull()?.takeIf { it in 0..999 }
            ?: error("hawkeyeVersionName component '$part' is not an integer in 0..999, from '$versionName'")
    }
    // The 100_000_000 radix overflows Google Play's version code cap of 2,100,000,000
    // once major exceeds 20.
    require(major <= 20) {
        "hawkeyeVersionName major '$major' pushes the version code past Google Play's cap, from '$versionName'"
    }
    val rc = when {
        suffix.isEmpty() && '-' !in versionName -> 99
        suffix == "dev" || suffix == "ci" -> 0
        suffix.matches(Regex("rc[1-9][0-9]?")) ->
            suffix.removePrefix("rc").toInt().also {
                require(it <= 98) { "rc99 collides with the final release's version code, from '$versionName'" }
            }
        else -> error("hawkeyeVersionName suffix '$suffix' is not rc1..rc98, dev, or ci, from '$versionName'")
    }
    // 0.0.0-dev (the local fallback) and CI's 0.0.0-ci both compute to 0, so the floor
    // of 1 covers them both.
    return (major * 100_000_000 + minor * 100_000 + patch * 100 + rc).coerceAtLeast(1)
}
