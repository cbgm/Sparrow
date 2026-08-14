package com.cbgm.securechat.feature.chats.data.group.incoming.handler

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.chats.data.group.membership.GroupMembershipCoordinator

class GroupInviteReceivedPacketHandler(
    private val membershipCoordinator: GroupMembershipCoordinator
) : GroupPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is GroupInviteReceivedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        membershipCoordinator.receiveInviteReceived(
            memberContactId = context.contactId,
            packet = packet as GroupInviteReceivedPacket,
            receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
        )
}
