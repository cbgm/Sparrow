package com.cbgm.sparrow.feature.invite.data.protocol.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.invite.data.group.GroupInviteDeclinedIncomingProcessor

internal class GroupInviteDeclinedPacketHandler(
    private val inviteDeclinedIncomingProcessor: GroupInviteDeclinedIncomingProcessor
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is GroupInviteDeclinedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        inviteDeclinedIncomingProcessor.process(
            memberContactId = context.contactId,
            packet = packet as GroupInviteDeclinedPacket,
            receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
        )
}
