package com.cbgm.sparrow.feature.invite.data.group

import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleStatus
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import kotlinx.coroutines.flow.Flow

internal class GroupInvitationLifecycleCoordinator(
    private val processor: GroupInvitationLifecycleProcessor
) {
    fun observeInvitations(direction: InvitationDirection): Flow<List<Invitation>> =
        processor.observeInvitations(direction)

    fun observeLifecycleStatus(
        payloadId: String,
        peerId: String,
        direction: InvitationDirection
    ): Flow<InvitationLifecycleStatus?> =
        processor.observeLifecycleStatus(
            payloadId = payloadId,
            peerId = peerId,
            direction = direction
        )

    fun observeInvitationResults(): Flow<List<InvitationResult>> =
        processor.observeInvitationResults()

    suspend fun contains(invitationId: String): Boolean =
        processor.contains(invitationId)

    suspend fun getPeerId(invitationId: String): Result<String> =
        processor.getPeerId(invitationId)

    suspend fun send(
        groupId: String,
        contactIds: Set<String>
    ): Result<Unit> = processor.send(groupId, contactIds)

    suspend fun markTransportFailed(packetId: String) {
        processor.markTransportFailed(packetId)
    }

    suspend fun accept(invitationId: String): Result<Unit> =
        processor.accept(invitationId)

    suspend fun decline(invitationId: String): Result<Unit> =
        processor.decline(invitationId)

    suspend fun deleteDeclinedOutgoing(invitationId: String): Result<Unit> =
        processor.deleteDeclinedOutgoing(invitationId)
}
