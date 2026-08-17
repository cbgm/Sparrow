package com.cbgm.sparrow.feature.identity.domain.repository

import com.cbgm.sparrow.feature.identity.domain.model.LocalProfilePicture
import kotlinx.coroutines.flow.Flow

interface LocalProfilePictureRepository {
    fun observe(): Flow<LocalProfilePicture>

    suspend fun get(): Result<LocalProfilePicture>

    suspend fun save(
        bytes: ByteArray,
        changedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun remove(changedAtEpochMilliseconds: Long): Result<Unit>
}
