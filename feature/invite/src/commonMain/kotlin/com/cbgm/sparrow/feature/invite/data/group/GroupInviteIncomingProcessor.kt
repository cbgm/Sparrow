package com.cbgm.sparrow.feature.invite.data.group

import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.feature.invite.data.protocol.GroupInvitationPacketProcessor

internal class GroupInviteIncomingProcessor(
    private val processor: GroupInvitationPacketProcessor
) {
    suspend fun process(
        ownerContactId: String,
        packet: GroupInvitePacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        processor.receiveInvite(
            ownerContactId = ownerContactId,
            packet = packet,
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
        )
}
