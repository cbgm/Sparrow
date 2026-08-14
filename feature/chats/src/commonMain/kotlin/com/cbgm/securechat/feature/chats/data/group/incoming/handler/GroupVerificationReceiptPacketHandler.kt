package com.cbgm.securechat.feature.chats.data.group.incoming.handler

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.chats.data.group.verification.GroupVerificationCoordinator

class GroupVerificationReceiptPacketHandler(
    private val coordinator: GroupVerificationCoordinator
) : GroupPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean =
        packet is GroupVerificationReceiptPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        coordinator.receiveReceipt(
            context = context,
            packet =
                packet as? GroupVerificationReceiptPacket
                    ?: error("Incompatible group verification receipt")
        )
}
