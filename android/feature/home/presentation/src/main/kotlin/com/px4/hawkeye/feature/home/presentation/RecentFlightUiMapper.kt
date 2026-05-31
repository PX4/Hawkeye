package com.px4.hawkeye.feature.home.presentation

import com.px4.hawkeye.core.domain.LibraryEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val recentDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US).withZone(ZoneId.systemDefault())

internal fun LibraryEntry.toRecentUi(): RecentFlightUi = RecentFlightUi(
    id = id,
    displayName = displayName,
    subtitle = recentDateFormatter.format(Instant.ofEpochMilli(importedAtMillis)),
)
