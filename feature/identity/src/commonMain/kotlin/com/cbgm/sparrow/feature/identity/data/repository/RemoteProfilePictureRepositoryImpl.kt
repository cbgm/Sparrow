package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.data.datasource.RemoteProfilePictureDataSource
import com.cbgm.sparrow.feature.identity.domain.model.RemoteProfilePicture
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteProfilePictureRepository
import kotlinx.coroutines.flow.Flow

class RemoteProfilePictureRepositoryImpl(
    private val dataSource: RemoteProfilePictureDataSource
) : RemoteProfilePictureRepository {
    override fun observe(contactId: String): Flow<RemoteProfilePicture> {
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }
        return dataSource.observe(contactId)
    }

    override suspend fun get(contactId: String): Result<RemoteProfilePicture> =
        safeSuspendCall {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            dataSource.get(contactId)
        }

    override suspend fun save(
        contactId: String,
        bytes: ByteArray,
        changedAtEpochMilliseconds: Long
    ): Result<Unit> =
        safeSuspendCall {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            require(bytes.isNotEmpty()) { "Remote profile picture must not be empty" }
            dataSource.save(
                contactId = contactId,
                bytes = bytes,
                changedAtEpochMilliseconds = changedAtEpochMilliseconds
            )
        }

    override suspend fun remove(
        contactId: String,
        changedAtEpochMilliseconds: Long
    ): Result<Unit> =
        safeSuspendCall {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            dataSource.remove(
                contactId = contactId,
                changedAtEpochMilliseconds = changedAtEpochMilliseconds
            )
        }
}
