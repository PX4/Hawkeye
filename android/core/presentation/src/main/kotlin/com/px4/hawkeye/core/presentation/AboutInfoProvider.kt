package com.px4.hawkeye.core.presentation

/**
 * Seam for build metadata and the bundled third-party notices. Feature modules cannot read
 * the application's package info or its assets — both belong to `:app` — so the app provides
 * an implementation through Koin and the About screen reads it from there.
 */
interface AboutInfoProvider {
    /** The installed build's `versionName`, or an empty string if it cannot be read. */
    val versionName: String

    /** Reads the packaged license notices. Throws if the assets cannot be read. */
    suspend fun loadNotices(): String
}
