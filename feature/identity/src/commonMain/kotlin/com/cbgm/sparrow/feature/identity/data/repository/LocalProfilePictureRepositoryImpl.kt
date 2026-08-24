package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.core.datastore.SparrowDataStore
import com.cbgm.sparrow.feature.identity.data.datasource.storage.ProfilePictureFileStorage
import com.cbgm.sparrow.feature.identity.domain.model.LocalProfilePicture
import com.cbgm.sparrow.feature.identity.domain.repository.LocalProfilePictureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalProfilePictureRepositoryImpl(
    private val dataStore: SparrowDataStore,
    private val fileStorage: ProfilePictureFileStorage
) : LocalProfilePictureRepository {
    override fun observe(): Flow<LocalProfilePicture> =
        dataStore.observeLong(PROFILE_PICTURE_CHANGED_AT).map { changedAt ->
            LocalProfilePicture(
                changedAtEpochMilliseconds = changedAt,
                bytes = fileStorage.readLocal()
            )
        }

    override suspend fun get(): Result<LocalProfilePicture> =
        runCatching {
            LocalProfilePicture(
                changedAtEpochMilliseconds = dataStore.getLong(PROFILE_PICTURE_CHANGED_AT),
                bytes = fileStorage.readLocal()
            )
        }

    override suspend fun save(
        bytes: ByteArray,
        changedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            require(bytes.isNotEmpty()) { "Profile picture must not be empty" }
            fileStorage.writeLocal(bytes)
            dataStore.edit { putLong(PROFILE_PICTURE_CHANGED_AT, changedAtEpochMilliseconds) }
        }

    override suspend fun remove(changedAtEpochMilliseconds: Long): Result<Unit> =
        runCatching {
            fileStorage.deleteLocal()
            dataStore.edit { putLong(PROFILE_PICTURE_CHANGED_AT, changedAtEpochMilliseconds) }
        }

    private companion object {
        const val PROFILE_PICTURE_CHANGED_AT = "identity.profile_picture.changed_at"
    }
}
