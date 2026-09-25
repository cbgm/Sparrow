package com.cbgm.sparrow.feature.media.data.datasource

import com.cbgm.sparrow.feature.media.data.model.StoredMediaSelectionPathsDto

interface MediaSelectionFileDataSource {
    suspend fun save(id: String, bytes: ByteArray, previewBytes: ByteArray?): StoredMediaSelectionPathsDto

    suspend fun read(localFilePath: String): ByteArray

    suspend fun delete(localFilePath: String)
}
