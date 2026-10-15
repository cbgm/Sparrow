package com.cbgm.sparrow.feature.media.domain.repository

import com.cbgm.sparrow.feature.media.domain.model.StoredMediaSelectionPaths

/** Selected, unsent attachments are kept in private app storage; not in UI state. */
interface MediaSelectionFileRepository {
    suspend fun save(
        id: String,
        bytes: ByteArray,
        previewBytes: ByteArray?
    ): Result<StoredMediaSelectionPaths>

    suspend fun read(localFilePath: String): Result<ByteArray>

    suspend fun delete(localFilePath: String): Result<Unit>
}
