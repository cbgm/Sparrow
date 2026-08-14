package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.chats.data.group.verification.GroupVerificationCoordinator

class GroupVerificationReceiptPacketHandler(
    private val coordinator: GroupVerificationCoordinator
) : GroupPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean =
        packet is GroupVerificationReceiptPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        coordinator.receiveReceipt(
            context = context,
            packet =
                packet as? GroupVerificationReceiptPacket
                    ?: error("Incompatible group verification receipt")
        )
}
