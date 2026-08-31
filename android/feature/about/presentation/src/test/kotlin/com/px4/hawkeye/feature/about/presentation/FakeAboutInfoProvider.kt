package com.px4.hawkeye.feature.about.presentation

import com.px4.hawkeye.core.presentation.AboutInfoProvider
import java.io.IOException

class FakeAboutInfoProvider(
    override val versionName: String = "1.2.3",
    private val notices: String = "notice text",
    private val failNotices: Boolean = false,
) : AboutInfoProvider {

    override suspend fun loadNotices(): String =
        if (failNotices) throw IOException("asset missing") else notices
}
