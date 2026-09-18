package com.cbgm.sparrow.feature.invite.data.repository

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
import kotlinx.coroutines.flow.combine

internal class InvitationRepositoryImpl(
    lifecycleDataSources: List<InvitationLifecycleDataSource>
) : InvitationRepository {
    private val dataSourcesByPayloadType = lifecycleDataSources.associateBy { it.payloadType }

    init {
        require(dataSourcesByPayloadType.size == lifecycleDataSources.size) {
            "Only one invitation lifecycle data source may be registered per payload type"
        }
    }

    override fun observeInvitations(direction: InvitationDirection): Flow<List<Invitation>> =
        combine(
            dataSource(InvitationPayloadType.DIRECT).observeInvitations(direction),
            dataSource(InvitationPayloadType.GROUP).observeInvitations(direction)
        ) { direct, group ->
            (direct + group).sortedByDescending(Invitation::updatedAtEpochMilliseconds)
        }

    override fun observeLifecycleStatus(
        payloadType: InvitationPayloadType,
        payloadId: String,
        peerId: String,
        direction: InvitationDirection
    ): Flow<InvitationLifecycleStatus?> =
        dataSource(payloadType).observeLifecycleStatus(
            payloadId = payloadId,
            peerId = peerId,
            direction = direction
        )

    override fun observeInvitationResults(): Flow<List<InvitationResult>> =
        combine(
            dataSource(InvitationPayloadType.DIRECT).observeInvitationResults(),
            dataSource(InvitationPayloadType.GROUP).observeInvitationResults()
        ) { direct, group -> direct + group }

    override suspend fun getPeerId(invitationId: String): Result<String> =
        findDataSource(invitationId)
            .getOrElse { return Result.failure(it) }
            .getPeerId(invitationId)

    override suspend fun shouldRecordPending(record: InvitationLifecycleRecord): Result<Boolean> =
        dataSource(record.payloadType).shouldRecordPending(record)

    override suspend fun recordPending(record: InvitationLifecycleRecord): Result<Unit> =
        dataSource(record.payloadType).recordPending(record)

    override suspend fun validatePending(
        payloadType: InvitationPayloadType,
        invitationId: String,
        payloadId: String,
        peerId: String,
        direction: InvitationDirection,
        atEpochMilliseconds: Long
    ): Result<Unit> =
        dataSource(payloadType).validatePending(
            invitationId = invitationId,
            payloadId = payloadId,
            peerId = peerId,
            direction = direction,
            atEpochMilliseconds = atEpochMilliseconds
        )

    override suspend fun getPayloadType(invitationId: String): Result<InvitationPayloadType> =
        findDataSource(invitationId).map { dataSource -> dataSource.payloadType }

    override suspend fun send(
        payloadType: InvitationPayloadType,
        payloadId: String,
        peerIds: Set<String>
    ): Result<Unit> = dataSource(payloadType).send(payloadId, peerIds)

    override suspend fun accept(invitationId: String): Result<Unit> =
        findDataSource(invitationId)
            .getOrElse { return Result.failure(it) }
            .accept(invitationId)

    override suspend fun decline(
        invitationId: String,
        action: InvitationResultAction?
    ): Result<Unit> =
        findDataSource(invitationId)
            .getOrElse { return Result.failure(it) }
            .decline(invitationId, action)

    override suspend fun applyResponse(
        payloadType: InvitationPayloadType,
        invitationId: String,
        response: InvitationResponse
    ): Result<Unit> =
        dataSource(payloadType).applyResponse(invitationId, response)

    override suspend fun markTransportFailed(
        payloadType: InvitationPayloadType,
        invitationId: String
    ): Result<Unit> =
        dataSource(payloadType).markTransportFailed(invitationId)

    override suspend fun markViewed(direction: InvitationDirection): Result<Unit> {
        for (dataSource in dataSourcesByPayloadType.values) {
            val result = dataSource.markViewed(direction)
            if (result.isFailure) return result
        }
        return Result.success(Unit)
    }

    override suspend fun deleteDeclinedOutgoing(invitationId: String): Result<Unit> =
        findDataSource(invitationId)
            .getOrElse { return Result.failure(it) }
            .deleteDeclinedOutgoing(invitationId)

    private fun dataSource(payloadType: InvitationPayloadType): InvitationLifecycleDataSource =
        dataSourcesByPayloadType[payloadType]
            ?: error("No invitation lifecycle data source registered for $payloadType")

    private suspend fun findDataSource(invitationId: String): Result<InvitationLifecycleDataSource> =
        runCatching {
            require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
            for (dataSource in dataSourcesByPayloadType.values) {
                if (dataSource.contains(invitationId)) return@runCatching dataSource
            }
            error("Invitation was not found: $invitationId")
        }
}
