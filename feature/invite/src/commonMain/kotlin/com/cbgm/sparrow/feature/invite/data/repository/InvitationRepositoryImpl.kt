package com.cbgm.sparrow.feature.invite.data.repository

import com.cbgm.sparrow.feature.invite.data.datasource.InvitationLifecycleDataSource
import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleStatus
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository
import kotlinx.coroutines.flow.Flow

internal class InvitationRepositoryImpl(
    private val dataSource: InvitationLifecycleDataSource
) : InvitationRepository {
    override fun observeInvitations(direction: InvitationDirection): Flow<List<Invitation>> =
        dataSource.observeInvitations(direction)

    override fun observeInvitationResults(): Flow<List<InvitationResult>> =
        dataSource.observeInvitationResults()

    override fun observeLifecycleStatus(
        payloadType: InvitationPayloadType,
        payloadId: String,
        peerId: String,
        direction: InvitationDirection
    ): Flow<InvitationLifecycleStatus?> =
        dataSource.observeLifecycleStatus(payloadType, payloadId, peerId, direction)

    override suspend fun getPeerId(invitationId: String): Result<String> =
        dataSource.getPeerId(invitationId)

    override suspend fun shouldRecordPending(record: InvitationLifecycleRecord): Result<Boolean> =
        dataSource.shouldRecordPending(record)

    override suspend fun recordPending(record: InvitationLifecycleRecord): Result<Unit> =
        dataSource.recordPending(record)

    override suspend fun validatePending(
        payloadType: InvitationPayloadType,
        invitationId: String,
        payloadId: String,
        peerId: String,
        direction: InvitationDirection,
        atEpochMilliseconds: Long
    ): Result<Unit> =
        dataSource.validatePending(
            payloadType = payloadType,
            invitationId = invitationId,
            payloadId = payloadId,
            peerId = peerId,
            direction = direction,
            atEpochMilliseconds = atEpochMilliseconds
        )

    override suspend fun getPayloadType(invitationId: String): Result<InvitationPayloadType> =
        dataSource.getPayloadType(invitationId)

    override suspend fun accept(invitationId: String): Result<Unit> =
        dataSource.accept(invitationId)

    override suspend fun decline(
        invitationId: String,
        action: InvitationResultAction?
    ): Result<Unit> =
        dataSource.decline(invitationId, action)

    override suspend fun applyResponse(
        payloadType: InvitationPayloadType,
        invitationId: String,
        response: InvitationResponse
    ): Result<Unit> =
        dataSource.applyResponse(payloadType, invitationId, response)

    override suspend fun invalidatePending(invitationId: String): Result<Unit> =
        dataSource.invalidatePending(invitationId)

    override suspend fun markTransportFailed(
        payloadType: InvitationPayloadType,
        invitationId: String
    ): Result<Unit> =
        dataSource.markTransportFailed(payloadType, invitationId)

    override suspend fun markViewed(direction: InvitationDirection): Result<Unit> =
        dataSource.markViewed(direction)

    override suspend fun deleteDeclinedOutgoing(invitationId: String): Result<Unit> =
        dataSource.deleteDeclinedOutgoing(invitationId)
}
