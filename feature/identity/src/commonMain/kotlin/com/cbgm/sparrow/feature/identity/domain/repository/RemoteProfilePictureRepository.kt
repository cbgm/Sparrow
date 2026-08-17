package com.cbgm.sparrow.feature.identity.domain.repository

import com.cbgm.sparrow.feature.identity.domain.model.RemoteProfilePicture
import kotlinx.coroutines.flow.Flow

interface RemoteProfilePictureRepository {
    fun observe(contactId: String): Flow<RemoteProfilePicture>

    suspend fun get(contactId: String): Result<RemoteProfilePicture>

    suspend fun save(
        contactId: String,
        bytes: ByteArray,
        changedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun remove(
        contactId: String,
        changedAtEpochMilliseconds: Long
    ): Result<Unit>
}
