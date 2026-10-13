package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.usecase.RecordPendingInvitationUseCase
import com.cbgm.sparrow.feature.membership.domain.model.StartedMembershipHandshake

/** Shows an outgoing group invitation in the invitation inbox until the member answers. */
class RecordGroupInvitationUseCase(
    private val recordPendingInvitation: RecordPendingInvitationUseCase
) {
    suspend operator fun invoke(started: StartedMembershipHandshake, groupTitle: String): Result<Unit> =
        recordPendingInvitation(
            InvitationLifecycleRecord(
                invitationId = started.sourceId,
                payloadType = InvitationPayloadType.GROUP,
                payloadId = started.groupId,
                peerId = started.peerId,
                direction = InvitationDirection.OUTGOING,
                createdAtEpochMilliseconds = started.createdAtEpochMilliseconds,
                expiresAtEpochMilliseconds = started.expiresAtEpochMilliseconds,
                updatedAtEpochMilliseconds = started.createdAtEpochMilliseconds,
                peerDisplayName = groupTitle
            )
        )
}
