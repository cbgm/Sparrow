package com.cbgm.sparrow.feature.invite.data.lifecycle

import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction

interface InvitationLifecycleEffects {
    val payloadType: InvitationPayloadType

    suspend fun send(
        payloadId: String,
        peerIds: Set<String>
    ): Result<List<InvitationLifecycleRecord>>

    suspend fun accept(invitationId: String): Result<Unit>

    suspend fun decline(
        invitationId: String,
        action: InvitationResultAction? = null
    ): Result<Unit>
}
