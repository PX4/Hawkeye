package com.px4.hawkeye.feature.home.presentation

import com.px4.hawkeye.core.domain.DataError
import com.px4.hawkeye.core.domain.EmptyResult
import com.px4.hawkeye.core.domain.LibraryEntry
import com.px4.hawkeye.core.domain.ReplayLibraryRepository
import com.px4.hawkeye.core.domain.Result
import kotlinx.coroutines.flow.MutableStateFlow

class FakeReplayLibraryRepository : ReplayLibraryRepository {

    val entriesFlow = MutableStateFlow<List<LibraryEntry>>(emptyList())
    var stageResult: EmptyResult<DataError.Local> = Result.Success(Unit)
    val stagedIds = mutableListOf<String>()

    override fun observeLibrary() = entriesFlow

    override suspend fun import(uri: String): Result<LibraryEntry, DataError.Local> =
        Result.Success(LibraryEntry("x", "x.ulg", 0L, 0L))

    override suspend fun delete(id: String): EmptyResult<DataError.Local> = Result.Success(Unit)

    override suspend fun stageForPlayback(id: String): EmptyResult<DataError.Local> {
        stagedIds += id
        return stageResult
    }
}
