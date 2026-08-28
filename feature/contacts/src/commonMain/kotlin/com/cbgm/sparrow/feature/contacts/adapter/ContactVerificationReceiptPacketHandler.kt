package com.cbgm.sparrow.feature.contacts.adapter

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.ContactVerificationReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.contacts.domain.usecase.HandleContactVerificationReceiptPacketUseCase

class ContactVerificationReceiptPacketHandler(
    private val handleContactVerificationReceiptPacket: HandleContactVerificationReceiptPacketUseCase
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is ContactVerificationReceiptPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        handleContactVerificationReceiptPacket(
            context = context,
            packet =
                packet as? ContactVerificationReceiptPacket
                    ?: error("Incompatible contact verification receipt")
        )
}
