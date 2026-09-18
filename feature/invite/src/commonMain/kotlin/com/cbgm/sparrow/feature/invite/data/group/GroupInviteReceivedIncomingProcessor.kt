package com.cbgm.sparrow.feature.invite.data.group

import com.cbgm.sparrow.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.feature.invite.data.protocol.GroupInvitationPacketProcessor

internal class GroupInviteReceivedIncomingProcessor(
    private val processor: GroupInvitationPacketProcessor
) {
    suspend fun process(
        memberContactId: String,
        packet: GroupInviteReceivedPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        processor.receiveInviteReceived(
            memberContactId = memberContactId,
            packet = packet,
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
        )
}
