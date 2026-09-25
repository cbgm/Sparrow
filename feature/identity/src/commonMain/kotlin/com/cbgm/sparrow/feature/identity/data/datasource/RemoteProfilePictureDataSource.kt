package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureMetadataProcessor
import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureProvider
import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureSnapshot
import com.cbgm.sparrow.data.datastore.SparrowDataStore
import com.cbgm.sparrow.feature.identity.domain.model.RemoteProfilePicture
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RemoteProfilePictureDataSource(
    private val dataStore: SparrowDataStore,
    private val fileDataSource: ProfilePictureFileDataSource,
    private val cryptoHash: CryptoHash
) : RemoteProfilePictureProvider,
    RemoteProfilePictureMetadataProcessor {
    private val metadataMutex = Mutex()

    fun observePicture(contactId: String): Flow<RemoteProfilePicture> =
        dataStore.observeLong(changedAtKey(contactId)).map { changedAt ->
            RemoteProfilePicture(
                contactId = contactId,
                changedAtEpochMilliseconds = changedAt,
                bytes = fileDataSource.readRemote(fileName(contactId))
            )
        }

    suspend fun get(contactId: String): RemoteProfilePicture = RemoteProfilePicture(
        contactId = contactId,
        changedAtEpochMilliseconds = dataStore.getLong(changedAtKey(contactId)),
        bytes = fileDataSource.readRemote(fileName(contactId))
    )

    suspend fun save(
        contactId: String,
        bytes: ByteArray,
        changedAtEpochMilliseconds: Long
    ) {
        fileDataSource.writeRemote(fileName(contactId), bytes)
        dataStore.edit { putLong(changedAtKey(contactId), changedAtEpochMilliseconds) }
    }

    suspend fun remove(
        contactId: String,
        changedAtEpochMilliseconds: Long
    ) {
        fileDataSource.deleteRemote(fileName(contactId))
        dataStore.edit { putLong(changedAtKey(contactId), changedAtEpochMilliseconds) }
    }

    override fun observe(contactId: String): Flow<RemoteProfilePictureSnapshot> =
        observePicture(contactId).map { picture ->
            RemoteProfilePictureSnapshot(
                contactId = picture.contactId,
                changedAtEpochMilliseconds = picture.changedAtEpochMilliseconds,
                bytes = picture.bytes
            )
        }

    override suspend fun apply(
        contactId: String,
        metadata: ProfilePictureMetadata
    ): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }

            metadataMutex.withLock {
                val current = get(contactId)
                if (metadata.changedAtEpochMilliseconds <= current.changedAtEpochMilliseconds) {
                    return@withLock
                }

                when {
                    !metadata.hasPicture ->
                        remove(
                            contactId = contactId,
                            changedAtEpochMilliseconds = metadata.changedAtEpochMilliseconds
                        )

                    metadata.payload != null ->
                        save(
                            contactId = contactId,
                            bytes = metadata.payload!!.bytes,
                            changedAtEpochMilliseconds = metadata.changedAtEpochMilliseconds
                        )

                    else -> Unit
                }
            }
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
