package com.cbgm.sparrow.feature.invite.data.group

import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.feature.invite.data.protocol.GroupInvitationPacketProcessor

internal class GroupJoinRequestIncomingProcessor(
    private val processor: GroupInvitationPacketProcessor
) {
    suspend fun process(
        memberContactId: String,
        packet: GroupJoinRequestPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        processor.receiveJoinRequest(
            memberContactId = memberContactId,
            packet = packet,
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
        )
}
