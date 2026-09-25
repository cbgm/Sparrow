package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.data.database.entity.ApprovedIdentityReconnectionEntity
import com.cbgm.sparrow.feature.identity.data.datasource.ApprovedIdentityReconnectionDataSource
import com.cbgm.sparrow.feature.identity.domain.model.ApprovedIdentityReconnection
import com.cbgm.sparrow.feature.identity.domain.repository.ApprovedIdentityReconnectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class ApprovedIdentityReconnectionRepositoryImpl(
    private val source: ApprovedIdentityReconnectionDataSource
) : ApprovedIdentityReconnectionRepository {
    override fun observeAll(): Flow<List<ApprovedIdentityReconnection>> =
        source.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun find(peerId: String): Result<ApprovedIdentityReconnection?> = safeSuspendCall {
        source.find(peerId)?.toDomain()
    }

    override suspend fun acknowledgeQueued(peerId: String, approvalId: String): Result<Unit> = safeSuspendCall {
        check(source.acknowledgeQueued(peerId, approvalId) == 1) { "Approval changed before acknowledging invitation" }
    }
}

private fun ApprovedIdentityReconnectionEntity.toDomain() = ApprovedIdentityReconnection(
    peerId = peerId,
    approvalId = approvalId,
    originalInviteChallenge = originalInviteChallenge?.copyOf(),
    originalInviteCreatedAtEpochMilliseconds = originalInviteCreatedAtEpochMilliseconds,
    originalInviteExpiresAtEpochMilliseconds = originalInviteExpiresAtEpochMilliseconds,
    originalInviteAutoSharesIdentity = originalInviteAutoSharesIdentity,
    originalInviterEncryptionPublicKey = originalInviterEncryptionPublicKey?.copyOf(),
    originalInviterSigningPublicKey = originalInviterSigningPublicKey?.copyOf(),
    approvedAtEpochMilliseconds = approvedAtEpochMilliseconds
)
