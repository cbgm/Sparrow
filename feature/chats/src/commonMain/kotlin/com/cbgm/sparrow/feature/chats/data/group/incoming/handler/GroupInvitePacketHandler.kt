package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipCoordinator

class GroupInvitePacketHandler(
    private val membershipCoordinator: GroupMembershipCoordinator
) : GroupPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is GroupInvitePacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        membershipCoordinator.receiveInvite(
            ownerContactId = context.contactId,
            packet = packet as GroupInvitePacket,
            receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
        )
}
