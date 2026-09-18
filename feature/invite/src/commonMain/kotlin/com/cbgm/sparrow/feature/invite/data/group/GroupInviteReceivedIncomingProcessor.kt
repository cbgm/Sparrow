package com.cbgm.sparrow.feature.invite.data.group

import com.cbgm.sparrow.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.feature.invite.data.protocol.GroupInvitationPacketProcessor
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.usecase.ValidatePendingInvitationUseCase

internal class GroupInviteReceivedIncomingProcessor(
    private val processor: GroupInvitationPacketProcessor,
    private val validatePendingInvitation: ValidatePendingInvitationUseCase
) {
    suspend fun process(
        memberContactId: String,
        packet: GroupInviteReceivedPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            validatePendingInvitation(
                payloadType = InvitationPayloadType.GROUP,
                invitationId = packet.invitationId,
                payloadId = packet.groupId,
                peerId = memberContactId,
                direction = InvitationDirection.OUTGOING,
                atEpochMilliseconds = receivedAtEpochMilliseconds
            ).getOrThrow()
            processor
                .receiveInviteReceived(
                    memberContactId = memberContactId,
                    packet = packet,
                    receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
                ).getOrThrow()
        }
}
