package com.cbgm.sparrow.feature.autoreply.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.autoreply.data.datasource.AutoReplyDataSource
import com.cbgm.sparrow.feature.autoreply.data.mapper.toDomain
import com.cbgm.sparrow.feature.autoreply.data.mapper.toEntity
import com.cbgm.sparrow.feature.autoreply.domain.model.AutoReply
import com.cbgm.sparrow.feature.autoreply.domain.repository.AutoReplyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AutoReplyRepositoryImpl(
    private val dataSource: AutoReplyDataSource
) : AutoReplyRepository {
    override fun observeAll(): Flow<List<AutoReply>> =
        dataSource.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeActive(): Flow<AutoReply?> =
        dataSource.observeActive().map { entity -> entity?.toDomain() }

    override suspend fun create(autoReply: AutoReply): Result<Unit> =
        safeSuspendCall {
            dataSource.upsert(autoReply.toEntity())
        }

    override suspend fun update(
        id: String,
        name: String,
        text: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit> =
        safeSuspendCall {
            dataSource.update(
                id = id,
                name = name,
                text = text,
                updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
            )
        }

    override suspend fun delete(id: String): Result<Unit> =
        safeSuspendCall { dataSource.delete(id) }

    override suspend fun activate(
        id: String,
        activationSessionId: String
    ): Result<Unit> =
        safeSuspendCall { dataSource.activate(id, activationSessionId) }

    override suspend fun deactivate(): Result<Unit> =
        safeSuspendCall { dataSource.deactivate() }

    override suspend fun claimForContact(
        contactId: String,
        sentAtEpochMilliseconds: Long
    ): Result<AutoReply?> =
        safeSuspendCall {
            dataSource
                .claimForContact(contactId, sentAtEpochMilliseconds)
                ?.toDomain()
        }

    override suspend fun releaseContactClaim(
        contactId: String,
        expectedActivationSessionId: String
    ): Result<Unit> =
        safeSuspendCall {
            dataSource.releaseContactClaim(contactId, expectedActivationSessionId)
        }
}
