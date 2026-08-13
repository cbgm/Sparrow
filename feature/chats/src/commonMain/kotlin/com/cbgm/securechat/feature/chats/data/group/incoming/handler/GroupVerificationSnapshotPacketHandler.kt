package com.cbgm.securechat.feature.chats.data.group.incoming.handler

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.chats.data.group.verification.GroupVerificationCoordinator

class GroupVerificationSnapshotPacketHandler(
    private val coordinator: GroupVerificationCoordinator
) : GroupPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean =
        packet is GroupVerificationSnapshotPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        coordinator.receiveSnapshot(
            context = context,
            packet =
                packet as? GroupVerificationSnapshotPacket
                    ?: error("Incompatible group verification snapshot")
        )
}
