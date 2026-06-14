package com.px4.hawkeye.core.domain

import kotlinx.coroutines.flow.Flow

/**
 * Coordinates the in-app log library's two sources of truth: persisted metadata and the
 * on-disk `.ulg` payloads. "Repository" is warranted because it combines those sources
 * rather than wrapping a single one.
 *
 * Lives in `core:domain` because both the Replay library and the Home recents peek depend
 * on it; the concrete implementation lives in `feature:replay:data`.
 */
interface ReplayLibraryRepository {

    /** Imported logs, newest first. Emits again whenever the library changes. */
    fun observeLibrary(): Flow<List<LibraryEntry>>

    /** Copies the document at [uri] into the library and records its metadata. */
    suspend fun import(uri: String): Result<LibraryEntry, DataError.Local>

    /** Removes both the metadata row and the on-disk payload for [id]. */
    suspend fun delete(id: String): EmptyResult<DataError.Local>

    /**
     * Removes the metadata rows and on-disk payloads for every id in [ids]. Resolves all ids
     * first: a single unknown id fails the whole batch (returns [DataError.Local.NOT_FOUND])
     * before anything is deleted, so a selection is never half-removed.
     */
    suspend fun deleteAll(ids: List<String>): EmptyResult<DataError.Local>

    /**
     * Copies the library payloads for [ids] into the renderer's inbox (list order = drone
     * order for a multi-drone session) and bumps the sentinel so the native poll loop loads
     * them on the next launch.
     */
    suspend fun stageForPlayback(ids: List<String>): EmptyResult<DataError.Local>
}
