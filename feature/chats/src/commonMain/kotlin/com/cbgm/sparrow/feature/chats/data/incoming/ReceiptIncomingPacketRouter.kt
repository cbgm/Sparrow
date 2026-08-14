package com.cbgm.sparrow.feature.chats.data.incoming

import com.cbgm.sparrow.core.protocol.packet.DeliveryReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.ReadReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.chats.data.direct.incoming.handler.DirectReceiptPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupReceiptPacketHandler
import com.cbgm.sparrow.feature.chats.data.model.DecodedIncomingPacket
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryEvent

/** Routes the shared receipt packet format to the owning conversation path. */
class ReceiptIncomingPacketRouter(
    private val directHandler: DirectReceiptPacketHandler,
    private val groupHandler: GroupReceiptPacketHandler
) {
    fun canRoute(packet: SparrowPacket): Boolean =
        packet is DeliveryReceiptPacket || packet is ReadReceiptPacket

    suspend fun route(incoming: DecodedIncomingPacket): Result<Unit> =
        runCatching {
            val receipt = incoming.packet.toReceipt()
            when {
                groupHandler.canHandle(receipt.messageId, incoming.contactId) ->
                    groupHandler.handle(
                        messageId = receipt.messageId,
                        contactId = incoming.contactId,
                        event = receipt.event
                    )

                directHandler.canHandle(receipt.messageId, incoming.contactId) ->
                    directHandler.handle(
                        messageId = receipt.messageId,
                        contactId = incoming.contactId,
                        event = receipt.event
                    )
            }
        }

    private fun SparrowPacket.toReceipt(): Receipt =
        when (this) {
            is DeliveryReceiptPacket ->
                Receipt(messageId, MessageDeliveryEvent.DELIVERY_CONFIRMED)

            is ReadReceiptPacket ->
                Receipt(messageId, MessageDeliveryEvent.READ_CONFIRMED)

            else -> error("Packet is not a receipt")
        }

    private data class Receipt(
        val messageId: String,
        val event: MessageDeliveryEvent
    )
}
