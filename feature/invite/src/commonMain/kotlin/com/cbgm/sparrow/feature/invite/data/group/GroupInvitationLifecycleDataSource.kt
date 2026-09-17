package com.cbgm.sparrow.feature.invite.data.group

import com.cbgm.sparrow.feature.invite.data.lifecycle.InvitationLifecycleDataSource
import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction
import kotlinx.coroutines.flow.Flow

internal class GroupInvitationLifecycleDataSource(
    private val coordinator: GroupInvitationLifecycleCoordinator
) : InvitationLifecycleDataSource {
    override val payloadType: InvitationPayloadType = InvitationPayloadType.GROUP

    override fun observeInvitations(direction: InvitationDirection): Flow<List<Invitation>> =
        coordinator.observeInvitations(direction)

    override fun observeInvitationResults(): Flow<List<InvitationResult>> =
        coordinator.observeInvitationResults()

    override suspend fun contains(invitationId: String): Boolean =
        coordinator.contains(invitationId)

    override suspend fun getPeerId(invitationId: String): Result<String> =
        coordinator.getPeerId(invitationId)

    override suspend fun send(
        payloadId: String,
        peerIds: Set<String>
    ): Result<Unit> = coordinator.send(payloadId, peerIds)

    override suspend fun accept(invitationId: String): Result<Unit> =
        coordinator.accept(invitationId)

    override suspend fun decline(
        invitationId: String,
        action: InvitationResultAction?
    ): Result<Unit> =
        if (action == null) {
            coordinator.decline(invitationId)
        } else {
            Result.failure(
                IllegalArgumentException("Group invitations do not support decline-and-block")
            )
        }

    override suspend fun applyResponse(
        invitationId: String,
        response: InvitationResponse
    ): Result<Unit> =
        Result.failure(
            IllegalArgumentException("Group invitation responses are handled by group packet processors")
        )

    override suspend fun markViewed(direction: InvitationDirection): Result<Unit> =
        Result.success(Unit)

    override suspend fun deleteDeclinedOutgoing(invitationId: String): Result<Unit> =
        coordinator.deleteDeclinedOutgoing(invitationId)
}
