package com.cbgm.securechat.feature.chats.data.group.incoming.handler

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.chats.data.group.membership.GroupMembershipCoordinator

class GroupInviteDeclinedPacketHandler(
    private val membershipCoordinator: GroupMembershipCoordinator
) : GroupPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is GroupInviteDeclinedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        membershipCoordinator.receiveDecline(
            memberContactId = context.contactId,
            packet = packet as GroupInviteDeclinedPacket,
            receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
        )
}
