package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.datastore.SparrowDataStore
import com.cbgm.sparrow.feature.identity.domain.model.RemoteProfilePicture
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RemoteProfilePictureDataSource(
    private val dataStore: SparrowDataStore,
    private val fileDataSource: ProfilePictureFileDataSource,
    private val cryptoHash: CryptoHash
) {
    fun observe(contactId: String): Flow<RemoteProfilePicture> =
        dataStore.observeLong(changedAtKey(contactId)).map { changedAt ->
            RemoteProfilePicture(
                contactId = contactId,
                changedAtEpochMilliseconds = changedAt,
                bytes = fileDataSource.readRemote(fileName(contactId))
            )
        }

    suspend fun get(contactId: String): Result<RemoteProfilePicture> =
        runCatching {
            RemoteProfilePicture(
                contactId = contactId,
                changedAtEpochMilliseconds = dataStore.getLong(changedAtKey(contactId)),
                bytes = fileDataSource.readRemote(fileName(contactId))
            )
        }

    suspend fun save(
        contactId: String,
        bytes: ByteArray,
        changedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            fileDataSource.writeRemote(fileName(contactId), bytes)
            dataStore.edit { putLong(changedAtKey(contactId), changedAtEpochMilliseconds) }
        }

    suspend fun remove(
        contactId: String,
        changedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            fileDataSource.deleteRemote(fileName(contactId))
            dataStore.edit { putLong(changedAtKey(contactId), changedAtEpochMilliseconds) }
        }

    private fun changedAtKey(contactId: String): String =
        "$REMOTE_PROFILE_PICTURE_CHANGED_AT_PREFIX$contactId"

    private fun fileName(contactId: String): String =
        cryptoHash
            .sha256(contactId.encodeToByteArray())
            .joinToString(separator = "") { byte ->
                (byte.toInt() and 0xff).toString(radix = 16).padStart(2, '0')
            } + ".jpg"

    private companion object {
        const val REMOTE_PROFILE_PICTURE_CHANGED_AT_PREFIX = "identity.remote_profile_picture.changed_at."
    }
}
