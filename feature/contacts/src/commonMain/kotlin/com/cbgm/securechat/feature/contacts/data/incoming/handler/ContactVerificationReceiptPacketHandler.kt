package com.cbgm.securechat.feature.contacts.data.incoming.handler

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.packet.ContactVerificationReceiptPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.contacts.data.repository.ContactVerificationRepositoryImpl

class ContactVerificationReceiptPacketHandler(
    private val coordinator: ContactVerificationRepositoryImpl
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is ContactVerificationReceiptPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        coordinator.receiveReceipt(
            context = context,
            packet = packet as? ContactVerificationReceiptPacket ?: error("Incompatible contact verification receipt")
        )
}
