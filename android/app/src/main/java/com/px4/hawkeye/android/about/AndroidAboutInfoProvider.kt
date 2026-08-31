package com.px4.hawkeye.android.about

import android.content.Context
import com.px4.hawkeye.core.presentation.AboutInfoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the installed build's version and the notice assets packaged with the APK. Both
 * assets are symlinks into the repository root, so what the screen shows is the same text
 * the source tree ships rather than a copy that can drift.
 */
class AndroidAboutInfoProvider(private val context: Context) : AboutInfoProvider {

    override val versionName: String
        get() = runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()

    override suspend fun loadNotices(): String = withContext(Dispatchers.IO) {
        NOTICE_ASSETS.joinToString(separator = "\n\n\n") { path ->
            context.assets.open(path).bufferedReader().use { it.readText() }.trim()
        }
    }

    private companion object {
        val NOTICE_ASSETS = listOf("NOTICE.md", "fonts/OFL.txt")
    }
}
