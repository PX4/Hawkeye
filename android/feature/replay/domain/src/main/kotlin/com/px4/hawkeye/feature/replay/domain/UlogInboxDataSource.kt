package com.px4.hawkeye.feature.replay.domain

import com.px4.hawkeye.core.domain.EmptyResult
import com.px4.hawkeye.core.domain.Result

interface UlogInboxDataSource {

    /**
     * Resolves the user-facing display name and a short "source" string (authority or path)
     * for an inbound .ulg URI. Used to populate the confirm-open dialog before ingestion.
     */
    suspend fun preview(uri: String): Result<UlogPreview, ReplayError>

    /**
     * Copies the URI's bytes into the inbox via a .tmp + atomic rename, then bumps
     * the .ready sentinel that the native poll loop watches.
     */
    suspend fun ingest(uri: String): Result<UlogFile, ReplayError>

    /**
     * Removes any previously-ingested payload and the sentinel. Used on cold launch
     * with no inbound intent so the native side starts at origin.
     */
    suspend fun clearInbox(): EmptyResult<ReplayError>
}
