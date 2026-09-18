package com.cbgm.sparrow.feature.invite.data.lifecycle

import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction

interface InvitationLifecycleEffects {
    val payloadType: InvitationPayloadType

    suspend fun send(
        payloadId: String,
        peerId: String
    ): Result<InvitationLifecycleRecord?>

    suspend fun accept(invitationId: String): Result<Unit>

    suspend fun decline(
        invitationId: String,
        action: InvitationResultAction? = null
    ): Result<Unit>

    suspend fun onExpired(invitationId: String): Result<Unit> = Result.success(Unit)

    suspend fun onTransportFailed(invitationId: String): Result<Unit> = Result.success(Unit)

    suspend fun onDeleteDeclinedOutgoing(invitationId: String): Result<Unit> = Result.success(Unit)
}
