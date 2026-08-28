package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.feature.identity.data.datasource.LocalProfilePictureDataSource
import com.cbgm.sparrow.feature.identity.domain.model.LocalProfilePicture
import com.cbgm.sparrow.feature.identity.domain.repository.LocalProfilePictureRepository
import kotlinx.coroutines.flow.Flow

class LocalProfilePictureRepositoryImpl(
    private val dataSource: LocalProfilePictureDataSource
) : LocalProfilePictureRepository {
    override fun observe(): Flow<LocalProfilePicture> = dataSource.observe()

    override suspend fun get(): Result<LocalProfilePicture> = dataSource.get()

    override suspend fun save(
        bytes: ByteArray,
        changedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            require(bytes.isNotEmpty()) { "Profile picture must not be empty" }
            dataSource.save(bytes = bytes, changedAtEpochMilliseconds = changedAtEpochMilliseconds).getOrThrow()
        }

    override suspend fun remove(changedAtEpochMilliseconds: Long): Result<Unit> =
        dataSource.remove(changedAtEpochMilliseconds)
}
