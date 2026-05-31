package com.px4.hawkeye.feature.replay.data

import com.px4.hawkeye.core.domain.DataError
import com.px4.hawkeye.core.domain.EmptyResult
import com.px4.hawkeye.core.domain.Result

class FakeReplayFileManager : ReplayFileManager {
    var importResult: Result<Long, DataError.Local> = Result.Success(2048L)
    var stageResult: EmptyResult<DataError.Local> = Result.Success(Unit)
    var displayName: String = "flight.ulg"

    val importedFileNames = mutableListOf<String>()
    val deletedFileNames = mutableListOf<String>()
    val stagedFileNames = mutableListOf<String>()

    override fun import(uri: String, fileName: String): Result<Long, DataError.Local> {
        importedFileNames += fileName
        return importResult
    }

    override fun stage(fileName: String): EmptyResult<DataError.Local> {
        stagedFileNames += fileName
        return stageResult
    }

    override fun delete(fileName: String) { deletedFileNames += fileName }

    override fun resolveDisplayName(uri: String): String = displayName
}
