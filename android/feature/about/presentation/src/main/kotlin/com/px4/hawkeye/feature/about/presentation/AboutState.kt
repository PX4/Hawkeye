package com.px4.hawkeye.feature.about.presentation

import androidx.compose.runtime.Stable

@Stable
data class AboutState(
    val versionName: String = "",
    /** Packaged notice text; null while loading or if the assets could not be read. */
    val notices: String? = null,
    val isLicensesExpanded: Boolean = false,
)
