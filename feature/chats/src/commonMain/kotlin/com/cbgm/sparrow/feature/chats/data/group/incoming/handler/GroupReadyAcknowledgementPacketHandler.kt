package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipCoordinator

class GroupReadyAcknowledgementPacketHandler(
    private val membershipCoordinator: GroupMembershipCoordinator
) : GroupPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is GroupReadyAcknowledgementPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        membershipCoordinator.receiveReadyAcknowledgement(
            memberContactId = context.contactId,
            packet = packet as GroupReadyAcknowledgementPacket,
            receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
        )
}
