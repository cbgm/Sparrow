package com.cbgm.sparrow.feature.invite.domain.repository

import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleStatus
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction
import kotlinx.coroutines.flow.Flow

interface InvitationRepository {
    fun observeInvitations(direction: InvitationDirection): Flow<List<Invitation>>

    fun observeInvitationResults(): Flow<List<InvitationResult>>

    fun observeLifecycleStatus(
        payloadType: InvitationPayloadType,
        payloadId: String,
        peerId: String,
        direction: InvitationDirection
    ): Flow<InvitationLifecycleStatus?>

    suspend fun getPeerId(invitationId: String): Result<String>

    suspend fun shouldRecordPending(record: InvitationLifecycleRecord): Result<Boolean>

    suspend fun recordPending(record: InvitationLifecycleRecord): Result<Unit>

    suspend fun validatePending(
        payloadType: InvitationPayloadType,
        invitationId: String,
        payloadId: String,
        peerId: String,
        direction: InvitationDirection,
        atEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun getPayloadType(invitationId: String): Result<InvitationPayloadType>

    suspend fun send(
        payloadType: InvitationPayloadType,
        payloadId: String,
        peerIds: Set<String>
    ): Result<Unit>

    suspend fun accept(invitationId: String): Result<Unit>

    suspend fun decline(
        invitationId: String,
        action: InvitationResultAction? = null
    ): Result<Unit>

    suspend fun applyResponse(
        payloadType: InvitationPayloadType,
        invitationId: String,
        response: InvitationResponse
    ): Result<Unit>

    suspend fun markTransportFailed(
        payloadType: InvitationPayloadType,
        invitationId: String
    ): Result<Unit>

    suspend fun markViewed(direction: InvitationDirection): Result<Unit>

    suspend fun deleteDeclinedOutgoing(invitationId: String): Result<Unit>
}
