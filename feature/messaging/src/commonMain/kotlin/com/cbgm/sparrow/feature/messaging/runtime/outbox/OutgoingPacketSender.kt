package com.cbgm.sparrow.feature.messaging.runtime.outbox

import com.cbgm.sparrow.core.crypto.transport.TransportPayloadCodec
import com.cbgm.sparrow.core.protocol.codec.PacketCodec
import com.cbgm.sparrow.core.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutboxItem
import com.cbgm.sparrow.core.protocol.transport.OutgoingWireAcceptance
import com.cbgm.sparrow.core.protocol.transport.OutgoingWireSender
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase

class OutgoingPacketSender(
    private val getContact: GetContactUseCase,
    private val transportPayloadFactory: OutgoingTransportPayloadFactory,
    private val transportPayloadCodec: TransportPayloadCodec,
    private val packetCodec: PacketCodec,
    private val recipientRoutingResolver: OutgoingRecipientRoutingResolver,
    private val outgoingWireSender: OutgoingWireSender,
    private val deliveryStateListener: OutboxDeliveryStateListener
) {
    suspend fun send(item: ProtocolOutboxItem): Result<OutgoingWireAcceptance> =
        runCatching {
            val contact =
                getContact(item.contactId).getOrThrow()
                    ?: error("Outbox contact was not found")
            val packet = packetCodec.decode(item.encodedPacket).getOrThrow()
            val transportPayload =
                transportPayloadFactory
                    .create(item.encodedPacket, packet, contact)
                    .getOrThrow()
            val encodedTransportPayload = transportPayloadCodec.encode(transportPayload)

            deliveryStateListener
                .onPrepared(
                    packetId = item.packetId,
                    encodedTransportPayload = encodedTransportPayload,
                    transportMode = transportPayload.mode.name
                ).getOrThrow()

            val recipientRoutingId = recipientRoutingResolver.resolve(item.contactId, packet)
            outgoingWireSender
                .sendWithAcceptance(recipientRoutingId, encodedTransportPayload)
                .getOrThrow()
        }
}
