package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.mapper

import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.membership.domain.model.IncomingMembershipOffer

internal fun IncomingMembershipOffer.toInvitationRecord(receivedAtEpochMilliseconds: Long): InvitationLifecycleRecord =
    InvitationLifecycleRecord(
        invitationId = sourceId,
        payloadType = InvitationPayloadType.GROUP,
        payloadId = groupId,
        peerId = peerId,
        direction = InvitationDirection.INCOMING,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
        expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
        updatedAtEpochMilliseconds = maxOf(createdAtEpochMilliseconds, receivedAtEpochMilliseconds),
        peerDisplayName = title
    )
