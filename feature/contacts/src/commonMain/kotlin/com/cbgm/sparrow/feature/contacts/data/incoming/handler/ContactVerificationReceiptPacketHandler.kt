package com.cbgm.sparrow.feature.contacts.data.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.ContactVerificationReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.contacts.data.repository.ContactVerificationRepositoryImpl

class ContactVerificationReceiptPacketHandler(
    private val coordinator: ContactVerificationRepositoryImpl
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is ContactVerificationReceiptPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        coordinator.receiveReceipt(
            context = context,
            packet = packet as? ContactVerificationReceiptPacket ?: error("Incompatible contact verification receipt")
        )
}
