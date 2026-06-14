package com.px4.hawkeye.feature.replay.presentation

import com.px4.hawkeye.core.domain.DataError
import com.px4.hawkeye.core.domain.EmptyResult
import com.px4.hawkeye.core.domain.Result
import com.px4.hawkeye.core.domain.LibraryEntry
import com.px4.hawkeye.core.domain.ReplayLibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeReplayLibraryRepository : ReplayLibraryRepository {

    val entriesFlow = MutableStateFlow<List<LibraryEntry>>(emptyList())

    var importResult: Result<LibraryEntry, DataError.Local> =
        Result.Success(LibraryEntry("new", "new.ulg", 10L, 0L))
    var stageResult: EmptyResult<DataError.Local> = Result.Success(Unit)
    var deleteResult: EmptyResult<DataError.Local> = Result.Success(Unit)

    val importedUris = mutableListOf<String>()
    val stagedBatches = mutableListOf<List<String>>()
    val deletedIds = mutableListOf<String>()

    override fun observeLibrary() = entriesFlow

    override suspend fun import(uri: String): Result<LibraryEntry, DataError.Local> {
        importedUris += uri
        return importResult
    }

    override suspend fun delete(id: String): EmptyResult<DataError.Local> {
        deletedIds += id
        if (deleteResult is Result.Success) {
            entriesFlow.value = entriesFlow.value.filterNot { it.id == id }
        }
        return deleteResult
    }

    override suspend fun stageForPlayback(ids: List<String>): EmptyResult<DataError.Local> {
        stagedBatches += ids
        return stageResult
    }
}
