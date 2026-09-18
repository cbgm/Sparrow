package com.cbgm.sparrow.feature.invite.data.lifecycle

import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleStatus
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction
import kotlinx.coroutines.flow.Flow

interface InvitationLifecycleDataSource {
    val payloadType: InvitationPayloadType

    fun observeInvitations(direction: InvitationDirection): Flow<List<Invitation>>

    fun observeInvitationResults(): Flow<List<InvitationResult>>

    fun observeLifecycleStatus(
        payloadId: String,
        peerId: String,
        direction: InvitationDirection
    ): Flow<InvitationLifecycleStatus?>

    suspend fun contains(invitationId: String): Boolean

    suspend fun shouldRecordPending(record: InvitationLifecycleRecord): Result<Boolean>

    suspend fun recordPending(record: InvitationLifecycleRecord): Result<Unit>

    suspend fun validatePending(
        invitationId: String,
        payloadId: String,
        peerId: String,
        direction: InvitationDirection,
        atEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun getPeerId(invitationId: String): Result<String>

    suspend fun send(
        payloadId: String,
        peerIds: Set<String>
    ): Result<Unit>

    suspend fun accept(invitationId: String): Result<Unit>

    suspend fun decline(
        invitationId: String,
        action: InvitationResultAction? = null
    ): Result<Unit>

    suspend fun applyResponse(
        invitationId: String,
        response: InvitationResponse
    ): Result<Unit>

    suspend fun markTransportFailed(invitationId: String): Result<Unit>

    suspend fun markViewed(direction: InvitationDirection): Result<Unit>

    suspend fun deleteDeclinedOutgoing(invitationId: String): Result<Unit>
}
