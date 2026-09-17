package com.cbgm.sparrow.feature.chats.data.direct.invitation

import com.cbgm.sparrow.feature.invite.data.lifecycle.InvitationLifecycleDataSource
import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction
import kotlinx.coroutines.flow.Flow

internal class DirectInvitationLifecycleDataSource(
    private val coordinator: DirectIdentityExchangeCoordinator
) : InvitationLifecycleDataSource {
    override val payloadType: InvitationPayloadType = InvitationPayloadType.DIRECT

    override fun observeInvitations(direction: InvitationDirection): Flow<List<Invitation>> =
        coordinator.observeInvitations(direction)

    override fun observeInvitationResults(): Flow<List<InvitationResult>> =
        coordinator.observeInvitationResults()

    override suspend fun contains(invitationId: String): Boolean =
        coordinator.containsInvitation(invitationId)

    override suspend fun getPeerId(invitationId: String): Result<String> =
        coordinator.getPeerId(invitationId)

    override suspend fun send(
        payloadId: String,
        peerIds: Set<String>
    ): Result<Unit> =
        runCatching {
            require(peerIds.size == 1) { "A direct invitation requires exactly one peer" }
            val peerId = peerIds.single()
            require(payloadId == peerId) { "Direct invitation payload ID must match its peer ID" }
            coordinator.start(peerId).getOrThrow()
        }

    override suspend fun accept(invitationId: String): Result<Unit> =
        coordinator.accept(invitationId)

    override suspend fun decline(
        invitationId: String,
        action: InvitationResultAction?
    ): Result<Unit> = coordinator.decline(invitationId, action)

    override suspend fun applyResponse(
        invitationId: String,
        response: InvitationResponse
    ): Result<Unit> = coordinator.applyResponse(invitationId, response)

    override suspend fun markViewed(direction: InvitationDirection): Result<Unit> =
        coordinator.markViewed(direction)

    override suspend fun deleteDeclinedOutgoing(invitationId: String): Result<Unit> =
        coordinator.deleteDeclinedOutgoing(invitationId)
}
