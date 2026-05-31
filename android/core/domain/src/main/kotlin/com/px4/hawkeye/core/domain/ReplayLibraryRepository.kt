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
     * Copies the library payload for [id] into the renderer's inbox and bumps the
     * sentinel so the native poll loop loads it on the next launch.
     */
    suspend fun stageForPlayback(id: String): EmptyResult<DataError.Local>
}
