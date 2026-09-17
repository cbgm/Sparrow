package com.cbgm.sparrow.feature.invite.data.lifecycle

import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction
import kotlinx.coroutines.flow.Flow

interface InvitationLifecycleDataSource {
    val payloadType: InvitationPayloadType

    fun observeInvitations(direction: InvitationDirection): Flow<List<Invitation>>

    fun observeInvitationResults(): Flow<List<InvitationResult>>

    suspend fun contains(invitationId: String): Boolean

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

    suspend fun markViewed(direction: InvitationDirection): Result<Unit>

    suspend fun deleteDeclinedOutgoing(invitationId: String): Result<Unit>
}
