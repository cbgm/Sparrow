package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.IdentityResult
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.usecase.RecordPendingInvitationUseCase

/** Shows a verified incoming direct invitation in the invitation inbox. */
class RecordIncomingDirectInvitationUseCase(
    private val recordPendingInvitation: RecordPendingInvitationUseCase
) {
    suspend operator fun invoke(result: IdentityResult, peerDisplayName: String?): Result<Unit> =
        recordPendingInvitation(
            InvitationLifecycleRecord(
                invitationId = result.exchangeId,
                payloadType = InvitationPayloadType.DIRECT,
                payloadId = result.peerId,
                peerId = result.peerId,
                direction = InvitationDirection.INCOMING,
                createdAtEpochMilliseconds = result.createdAtEpochMilliseconds,
                expiresAtEpochMilliseconds = result.expiresAtEpochMilliseconds,
                updatedAtEpochMilliseconds =
                    maxOf(result.createdAtEpochMilliseconds, result.updatedAtEpochMilliseconds),
                peerDisplayName = peerDisplayName
            )
        )
}
