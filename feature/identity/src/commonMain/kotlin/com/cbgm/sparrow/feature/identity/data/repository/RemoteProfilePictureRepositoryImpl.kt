package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.datastore.SparrowDataStore
import com.cbgm.sparrow.feature.identity.data.datasource.storage.ProfilePictureFileStorage
import com.cbgm.sparrow.feature.identity.domain.model.RemoteProfilePicture
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteProfilePictureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RemoteProfilePictureRepositoryImpl(
    private val dataStore: SparrowDataStore,
    private val fileStorage: ProfilePictureFileStorage,
    private val cryptoHash: CryptoHash
) : RemoteProfilePictureRepository {
    override fun observe(contactId: String): Flow<RemoteProfilePicture> {
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }
        return dataStore.observeLong(changedAtKey(contactId)).map { changedAt ->
            RemoteProfilePicture(
                contactId = contactId,
                changedAtEpochMilliseconds = changedAt,
                bytes = fileStorage.readRemote(fileName(contactId))
            )
        }
    }

    override suspend fun get(contactId: String): Result<RemoteProfilePicture> =
        runCatching {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            RemoteProfilePicture(
                contactId = contactId,
                changedAtEpochMilliseconds = dataStore.getLong(changedAtKey(contactId)),
                bytes = fileStorage.readRemote(fileName(contactId))
            )
        }

    override suspend fun save(
        contactId: String,
        bytes: ByteArray,
        changedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            require(bytes.isNotEmpty()) { "Remote profile picture must not be empty" }
            fileStorage.writeRemote(fileName(contactId), bytes)
            dataStore.edit { putLong(changedAtKey(contactId), changedAtEpochMilliseconds) }
        }

    override suspend fun remove(
        contactId: String,
        changedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            fileStorage.deleteRemote(fileName(contactId))
            dataStore.edit { putLong(changedAtKey(contactId), changedAtEpochMilliseconds) }
        }

    private fun changedAtKey(contactId: String): String =
        "$REMOTE_PROFILE_PICTURE_CHANGED_AT_PREFIX$contactId"

    private fun fileName(contactId: String): String =
        cryptoHash
            .sha256(contactId.encodeToByteArray())
            .joinToString(separator = "") { byte -> (byte.toInt() and 0xff).toString(radix = 16).padStart(2, '0') } + ".jpg"

    private companion object {
        const val REMOTE_PROFILE_PICTURE_CHANGED_AT_PREFIX = "identity.remote_profile_picture.changed_at."
    }
}
