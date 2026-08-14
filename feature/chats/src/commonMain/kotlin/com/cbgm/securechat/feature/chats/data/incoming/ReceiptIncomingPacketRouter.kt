package com.cbgm.securechat.feature.chats.data.incoming

import com.cbgm.securechat.core.protocol.packet.DeliveryReceiptPacket
import com.cbgm.securechat.core.protocol.packet.ReadReceiptPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.chats.data.direct.incoming.handler.DirectReceiptPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupReceiptPacketHandler
import com.cbgm.securechat.feature.chats.data.model.DecodedIncomingPacket
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryEvent

/** Routes the shared receipt packet format to the owning conversation path. */
class ReceiptIncomingPacketRouter(
    private val directHandler: DirectReceiptPacketHandler,
    private val groupHandler: GroupReceiptPacketHandler
) {
    fun canRoute(packet: SecureChatPacket): Boolean =
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

    private fun SecureChatPacket.toReceipt(): Receipt =
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
