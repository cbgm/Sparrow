package com.cbgm.sparrow.feature.invite.data.group

import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleStatus
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import kotlinx.coroutines.flow.Flow

interface GroupInvitationLifecycleProcessor {
    fun observeInvitations(direction: InvitationDirection): Flow<List<Invitation>>

    fun observeLifecycleStatus(
        payloadId: String,
        peerId: String,
        direction: InvitationDirection
    ): Flow<InvitationLifecycleStatus?>

    fun observeInvitationResults(): Flow<List<InvitationResult>>

    suspend fun contains(invitationId: String): Boolean

    suspend fun getPeerId(invitationId: String): Result<String>

    suspend fun send(
        groupId: String,
        contactIds: Set<String>
    ): Result<Unit>

    suspend fun markTransportFailed(packetId: String)

    suspend fun accept(invitationId: String): Result<Unit>

    suspend fun decline(invitationId: String): Result<Unit>

    suspend fun deleteDeclinedOutgoing(invitationId: String): Result<Unit>
}
