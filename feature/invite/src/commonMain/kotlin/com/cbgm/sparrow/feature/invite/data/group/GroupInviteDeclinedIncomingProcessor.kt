package com.cbgm.sparrow.feature.invite.data.group

import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.feature.invite.data.protocol.GroupInvitationPacketProcessor

internal class GroupInviteDeclinedIncomingProcessor(
    private val processor: GroupInvitationPacketProcessor
) {
    suspend fun process(
        memberContactId: String,
        packet: GroupInviteDeclinedPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        processor.receiveDeclined(
            memberContactId = memberContactId,
            packet = packet,
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
        )
}
