package com.cbgm.sparrow.feature.media.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.media.data.datasource.MediaSelectionFileDataSource
import com.cbgm.sparrow.feature.media.data.mapper.toStoredMediaSelectionPaths
import com.cbgm.sparrow.feature.media.domain.model.StoredMediaSelectionPaths
import com.cbgm.sparrow.feature.media.domain.repository.MediaSelectionFileRepository

class MediaSelectionFileRepositoryImpl(
    private val dataSource: MediaSelectionFileDataSource
) : MediaSelectionFileRepository {
    override suspend fun save(
        id: String,
        bytes: ByteArray,
        previewBytes: ByteArray?
    ): Result<StoredMediaSelectionPaths> =
        safeSuspendCall { dataSource.save(id, bytes, previewBytes).toStoredMediaSelectionPaths() }

    override suspend fun read(localFilePath: String): Result<ByteArray> =
        safeSuspendCall { dataSource.read(localFilePath) }

    override suspend fun delete(localFilePath: String): Result<Unit> =
        safeSuspendCall { dataSource.delete(localFilePath) }
}
