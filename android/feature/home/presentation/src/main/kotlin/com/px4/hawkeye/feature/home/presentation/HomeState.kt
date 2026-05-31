package com.px4.hawkeye.feature.home.presentation

import androidx.compose.runtime.Stable

@Stable
data class HomeState(
    val recents: List<RecentFlightUi> = emptyList(),
)

/** A recent library entry as shown in the Home peek: name plus an imported-date subtitle. */
data class RecentFlightUi(
    val id: String,
    val displayName: String,
    val subtitle: String,
)
