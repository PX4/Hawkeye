package com.px4.hawkeye.feature.replay.presentation

import com.px4.hawkeye.core.domain.EmptyResult
import com.px4.hawkeye.core.domain.Result
import com.px4.hawkeye.feature.replay.domain.ReplayError
import com.px4.hawkeye.feature.replay.domain.UlogFile
import com.px4.hawkeye.feature.replay.domain.UlogInboxDataSource
import com.px4.hawkeye.feature.replay.domain.UlogPreview

class FakeUlogInboxDataSource : UlogInboxDataSource {

    var previewResult: Result<UlogPreview, ReplayError> =
        Result.Success(UlogPreview(displayName = "flight.ulg", source = "fakeauthority"))

    var ingestResult: Result<UlogFile, ReplayError> =
        Result.Success(UlogFile(displayName = "flight.ulg", sizeBytes = 1024L))

    var clearInboxCount: Int = 0
    val ingestedUris: MutableList<String> = mutableListOf()
    val previewedUris: MutableList<String> = mutableListOf()

    override suspend fun preview(uri: String): Result<UlogPreview, ReplayError> {
        previewedUris += uri
        return previewResult
    }

    override suspend fun ingest(uri: String): Result<UlogFile, ReplayError> {
        ingestedUris += uri
        return ingestResult
    }

    override suspend fun clearInbox(): EmptyResult<ReplayError> {
        clearInboxCount += 1
        return Result.Success(Unit)
    }
}
