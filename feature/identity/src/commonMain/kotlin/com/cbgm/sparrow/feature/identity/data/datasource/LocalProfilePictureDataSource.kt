package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import com.cbgm.sparrow.feature.identity.domain.model.LocalProfilePicture
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalProfilePictureDataSource(
    private val dataStore: SparrowDataStore,
    private val fileDataSource: ProfilePictureFileDataSource
) {
    fun observe(): Flow<LocalProfilePicture> =
        dataStore.observeLong(PROFILE_PICTURE_CHANGED_AT).map { changedAt ->
            LocalProfilePicture(
                changedAtEpochMilliseconds = changedAt,
                bytes = fileDataSource.readLocal()
            )
        }

    suspend fun get(): Result<LocalProfilePicture> =
        runCatching {
            LocalProfilePicture(
                changedAtEpochMilliseconds = dataStore.getLong(PROFILE_PICTURE_CHANGED_AT),
                bytes = fileDataSource.readLocal()
            )
        }

    suspend fun save(
        bytes: ByteArray,
        changedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            fileDataSource.writeLocal(bytes)
            dataStore.edit { putLong(PROFILE_PICTURE_CHANGED_AT, changedAtEpochMilliseconds) }
        }

    suspend fun remove(changedAtEpochMilliseconds: Long): Result<Unit> =
        runCatching {
            fileDataSource.deleteLocal()
            dataStore.edit { putLong(PROFILE_PICTURE_CHANGED_AT, changedAtEpochMilliseconds) }
        }

    private companion object {
        const val PROFILE_PICTURE_CHANGED_AT = "identity.profile_picture.changed_at"
    }
}
