package com.cbgm.sparrow.feature.conversationorchestration.runtime.outbox

import com.cbgm.sparrow.core.crypto.transport.TransportPayloadCodec
import com.cbgm.sparrow.core.protocol.codec.PacketCodec
import com.cbgm.sparrow.core.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutboxItem
import com.cbgm.sparrow.core.protocol.transport.OutgoingWireAcceptance
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObservePendingRemoteIdentityChangesUseCase
import com.cbgm.sparrow.feature.messaging.domain.usecase.SendEncodedTransportUseCase
import kotlinx.coroutines.flow.first

class OutgoingPacketSender(
    private val getContact: GetContactUseCase,
    private val transportPayloadFactory: OutgoingTransportPayloadFactory,
    private val transportPayloadCodec: TransportPayloadCodec,
    private val packetCodec: PacketCodec,
    private val recipientRoutingResolver: OutgoingRecipientRoutingResolver,
    private val sendEncodedTransport: SendEncodedTransportUseCase,
    private val deliveryStateListener: OutboxDeliveryStateListener,
    private val observePendingRemoteIdentityChanges: ObservePendingRemoteIdentityChangesUseCase
) {
    suspend fun send(item: ProtocolOutboxItem): Result<OutgoingWireAcceptance> =
        runCatching {
            val contact =
                getContact(item.contactId).getOrThrow()
                    ?: error("Outbox contact was not found")
            val packet = packetCodec.decode(item.encodedPacket).getOrThrow()
            // Existing outbox packets can predate an incoming identity-change
            // request. Check again AT THE TRANSPORT BOUNDARY so queued text,
            // reactions, receipts, edits and deletions cannot be prepared/sent
            // using the old trust state during independent identity review.
            // Fresh signed invitations remain possible; never auto-accept keys.
            if (blocksDirectPacketDuringIdentityReview(packet::class)) {
                check(observePendingRemoteIdentityChanges().first().none { it.peerId == item.contactId }) {
                    "Direct packet is blocked pending recipient identity verification"
                }
            }
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
            sendEncodedTransport(recipientRoutingId, encodedTransportPayload)
                .getOrElse { error ->
                    // Only a failure at the actual wire-send stage is retryable.
                    // Key lookup, encryption and routing-resolution errors must
                    // never be converted into automatic transport retries.
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    throw RetryableWireDeliveryException(error)
                }
        }
}
