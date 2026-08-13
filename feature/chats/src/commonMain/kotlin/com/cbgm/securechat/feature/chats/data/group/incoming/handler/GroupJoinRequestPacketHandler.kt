package com.cbgm.securechat.feature.chats.data.group.incoming.handler

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.chats.data.group.membership.GroupMembershipCoordinator

class GroupJoinRequestPacketHandler(
    private val membershipCoordinator: GroupMembershipCoordinator
) : GroupPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is GroupJoinRequestPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        membershipCoordinator.receiveJoinRequest(
            memberContactId = context.contactId,
            packet = packet as GroupJoinRequestPacket,
            receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
        )
}
