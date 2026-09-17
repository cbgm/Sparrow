package com.cbgm.sparrow.feature.invite.data.protocol.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.invite.data.group.GroupInviteReceivedIncomingProcessor

internal class GroupInviteReceivedPacketHandler(
    private val inviteReceivedIncomingProcessor: GroupInviteReceivedIncomingProcessor
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is GroupInviteReceivedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        inviteReceivedIncomingProcessor.process(
            memberContactId = context.contactId,
            packet = packet as GroupInviteReceivedPacket,
            receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
        )
}
