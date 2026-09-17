package com.cbgm.sparrow.feature.invite.data.repository

import com.cbgm.sparrow.feature.invite.data.direct.DirectIdentityExchangeCoordinator
import com.cbgm.sparrow.feature.invite.data.group.GroupInvitationLifecycleCoordinator
import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class InvitationRepositoryImpl(
    private val directCoordinator: DirectIdentityExchangeCoordinator,
    private val groupCoordinator: GroupInvitationLifecycleCoordinator
) : InvitationRepository {
    override fun observeInvitations(direction: InvitationDirection): Flow<List<Invitation>> =
        combine(
            directCoordinator.observeInvitations(direction),
            groupCoordinator.observeInvitations(direction)
        ) { direct, group ->
            (direct + group).sortedByDescending(Invitation::updatedAtEpochMilliseconds)
        }

    override fun observeInvitationResults(): Flow<List<InvitationResult>> =
        combine(
            directCoordinator.observeInvitationResults(),
            groupCoordinator.observeInvitationResults()
        ) { direct, group -> direct + group }

    override suspend fun getPeerId(invitationId: String): Result<String> =
        if (directCoordinator.containsInvitation(invitationId)) {
            directCoordinator.getPeerId(invitationId)
        } else {
            groupCoordinator.getPeerId(invitationId)
        }

    override suspend fun getPayloadType(invitationId: String): Result<InvitationPayloadType> =
        runCatching {
            if (directCoordinator.containsInvitation(invitationId)) {
                InvitationPayloadType.DIRECT
            } else if (groupCoordinator.contains(invitationId)) {
                InvitationPayloadType.GROUP
            } else {
                error("Invitation was not found")
            }
        }

    override suspend fun send(
        payloadType: InvitationPayloadType,
        payloadId: String,
        peerIds: Set<String>
    ): Result<Unit> =
        when (payloadType) {
            InvitationPayloadType.DIRECT ->
                runCatching {
                    require(peerIds.size == 1) { "A direct invitation requires exactly one peer" }
                    val peerId = peerIds.single()
                    require(payloadId == peerId) { "Direct invitation payload ID must match its peer ID" }
                    directCoordinator.start(peerId).getOrThrow()
                }

            InvitationPayloadType.GROUP -> groupCoordinator.send(payloadId, peerIds)
        }

    override suspend fun accept(invitationId: String): Result<Unit> =
        if (directCoordinator.containsInvitation(invitationId)) {
            directCoordinator.accept(invitationId)
        } else {
            groupCoordinator.accept(invitationId)
        }

    override suspend fun decline(
        invitationId: String,
        action: InvitationResultAction?
    ): Result<Unit> =
        if (directCoordinator.containsInvitation(invitationId)) {
            directCoordinator.decline(invitationId, action)
        } else {
            if (action != null) {
                Result.failure(IllegalArgumentException("Group invitations do not support decline-and-block"))
            } else {
                groupCoordinator.decline(invitationId)
            }
        }

    override suspend fun applyResponse(
        invitationId: String,
        response: InvitationResponse
    ): Result<Unit> =
        if (directCoordinator.containsInvitation(invitationId)) {
            directCoordinator.applyResponse(invitationId, response)
        } else {
            Result.failure(IllegalArgumentException("Group invitation responses are handled by group packet processors"))
        }

    override suspend fun markViewed(direction: InvitationDirection): Result<Unit> =
        directCoordinator.markViewed(direction)

    override suspend fun deleteDeclinedOutgoing(invitationId: String): Result<Unit> =
        if (directCoordinator.containsInvitation(invitationId)) {
            directCoordinator.deleteDeclinedOutgoing(invitationId)
        } else {
            groupCoordinator.deleteDeclinedOutgoing(invitationId)
        }
}
