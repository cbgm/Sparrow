package com.cbgm.securechat.feature.chats.data.group.incoming.handler

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.packet.GroupInvitePacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.chats.data.group.membership.GroupMembershipCoordinator

class GroupInvitePacketHandler(
    private val membershipCoordinator: GroupMembershipCoordinator
) : GroupPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is GroupInvitePacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        membershipCoordinator.receiveInvite(
            ownerContactId = context.contactId,
            packet = packet as GroupInvitePacket,
            receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
        )
}
