package com.cbgm.securechat.feature.chats.data.group.incoming.handler

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.packet.GroupVerificationSnapshotRequestPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.chats.data.group.verification.GroupVerificationCoordinator

class GroupVerificationSnapshotRequestPacketHandler(
    private val coordinator: GroupVerificationCoordinator
) : GroupPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean =
        packet is GroupVerificationSnapshotRequestPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        coordinator.receiveSnapshotRequest(
            context = context,
            packet =
                packet as? GroupVerificationSnapshotRequestPacket
                    ?: error("Incompatible group verification snapshot request")
        )
}
