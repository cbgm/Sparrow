package com.cbgm.sparrow.feature.media.data.repository

import com.cbgm.sparrow.feature.media.data.datasource.MediaSelectionFileDataSource
import com.cbgm.sparrow.feature.media.data.mapper.toStoredMediaSelectionPaths
import com.cbgm.sparrow.feature.media.domain.model.StoredMediaSelectionPaths
import com.cbgm.sparrow.feature.media.domain.repository.MediaSelectionFileRepository

class MediaSelectionFileRepositoryImpl(
    private val dataSource: MediaSelectionFileDataSource
) : MediaSelectionFileRepository {
    override suspend fun save(id: String, bytes: ByteArray, previewBytes: ByteArray?): StoredMediaSelectionPaths =
        dataSource.save(id, bytes, previewBytes).toStoredMediaSelectionPaths()

    override suspend fun read(localFilePath: String): ByteArray = dataSource.read(localFilePath)

    override suspend fun delete(localFilePath: String) = dataSource.delete(localFilePath)
}
