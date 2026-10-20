package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.data.database.dao.ContactProfilePictureDao
import com.cbgm.sparrow.data.database.entity.ContactProfilePictureEntity
import com.cbgm.sparrow.feature.identity.domain.model.RemoteProfilePicture
import com.cbgm.sparrow.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.protocol.profile.RemoteProfilePictureMetadataProcessor
import com.cbgm.sparrow.protocol.profile.RemoteProfilePictureProvider
import com.cbgm.sparrow.protocol.profile.RemoteProfilePictureSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RemoteProfilePictureDataSource(
    private val profilePictureDao: ContactProfilePictureDao,
    private val fileDataSource: ProfilePictureFileDataSource,
    private val cryptoHash: CryptoHash
) : RemoteProfilePictureProvider,
    RemoteProfilePictureMetadataProcessor {
    private val metadataMutex = Mutex()

    fun observePicture(contactId: String): Flow<RemoteProfilePicture> =
        profilePictureDao.observe(contactId).distinctUntilChanged().map { entity -> entity.toPicture(contactId) }

    suspend fun get(contactId: String): RemoteProfilePicture = profilePictureDao.get(contactId).toPicture(contactId)

    suspend fun save(
        contactId: String,
        bytes: ByteArray,
        changedAtEpochMilliseconds: Long
    ) {
        fileDataSource.writeRemote(fileName(contactId), bytes)
        profilePictureDao.upsert(ContactProfilePictureEntity(contactId, true, changedAtEpochMilliseconds))
    }

    suspend fun remove(
        contactId: String,
        changedAtEpochMilliseconds: Long
    ) {
        fileDataSource.deleteRemote(fileName(contactId))
        profilePictureDao.upsert(ContactProfilePictureEntity(contactId, false, changedAtEpochMilliseconds))
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

    private fun ContactProfilePictureEntity?.toPicture(contactId: String): RemoteProfilePicture =
        RemoteProfilePicture(
            contactId = contactId,
            changedAtEpochMilliseconds = this?.changedAtEpochMilliseconds ?: 0L,
            bytes = if (this?.hasPicture == true) fileDataSource.readRemote(fileName(contactId)) else null
        )

    private fun fileName(contactId: String): String =
        cryptoHash
            .sha256(contactId.encodeToByteArray())
            .joinToString(separator = "") { byte ->
                (byte.toInt() and 0xff).toString(radix = 16).padStart(2, '0')
            } + ".jpg"
}
