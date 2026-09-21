package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.data.database.entity.PendingRemoteIdentityChangeEntity
import com.cbgm.sparrow.feature.identity.data.datasource.PendingRemoteIdentityChangeDataSource
import com.cbgm.sparrow.feature.identity.domain.model.PendingRemoteIdentityChange
import com.cbgm.sparrow.feature.identity.domain.repository.PendingRemoteIdentityChangeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class PendingRemoteIdentityChangeRepositoryImpl(
    private val source: PendingRemoteIdentityChangeDataSource
) : PendingRemoteIdentityChangeRepository {
    override suspend fun stage(candidate: PendingRemoteIdentityChange): Result<Unit> = safeSuspendCall {
        require(candidate.peerId.isNotBlank() && candidate.sourcePeerId.isNotBlank())
        require(candidate.invitationId.isNotBlank())
        require(candidate.proposedEncryptionPublicKey.size == 32 && candidate.proposedSigningPublicKey.size == 32)
        require(candidate.expiresAtEpochMilliseconds > candidate.receivedAtEpochMilliseconds)
        // Replays must not push an older candidate over a more recent signed invitation.
        val current = source.find(candidate.peerId)
        if (current == null ||
            (
                current.invitationId == candidate.invitationId &&
                    current.proposedEncryptionPublicKey.contentEquals(candidate.proposedEncryptionPublicKey) &&
                    current.proposedSigningPublicKey.contentEquals(candidate.proposedSigningPublicKey)
            ) ||
            candidate.receivedAtEpochMilliseconds > current.receivedAtEpochMilliseconds
        ) {
            source.upsert(candidate.toEntity())
        }
    }

    override fun observeAll(): Flow<List<PendingRemoteIdentityChange>> =
        source.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun discard(peerId: String, invitationId: String): Result<Unit> = safeSuspendCall {
        source.discard(peerId, invitationId)
        Unit
    }
}

private fun PendingRemoteIdentityChange.toEntity() = PendingRemoteIdentityChangeEntity(
    peerId,
    sourcePeerId,
    invitationId,
    proposedEncryptionPublicKey.copyOf(),
    proposedSigningPublicKey.copyOf(),
    receivedAtEpochMilliseconds,
    expiresAtEpochMilliseconds
)

private fun PendingRemoteIdentityChangeEntity.toDomain() = PendingRemoteIdentityChange(
    peerId,
    sourcePeerId,
    invitationId,
    proposedEncryptionPublicKey.copyOf(),
    proposedSigningPublicKey.copyOf(),
    receivedAtEpochMilliseconds,
    expiresAtEpochMilliseconds
)
