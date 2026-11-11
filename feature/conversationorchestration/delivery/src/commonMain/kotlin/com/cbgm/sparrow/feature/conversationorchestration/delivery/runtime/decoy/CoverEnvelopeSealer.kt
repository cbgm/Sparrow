package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.decoy

import com.cbgm.sparrow.core.crypto.transport.TransportPayloadCodec
import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.outbox.OutgoingTransportPayloadFactory
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.protocol.codec.PacketCodec
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository
import com.cbgm.sparrow.protocol.packet.CoverPacket

/** Seals a decoy for one contact; null when the contact has no route that lasts or cannot be found. */
fun interface DecoySealer {
    suspend fun seal(
        contactId: String,
        createdAtEpochMilliseconds: Long
    ): FederatedEnvelope?
}

/** A [CoverPacket] for the contact's mailbox, encrypted, padded and given the lifetime of a message. */
class CoverEnvelopeSealer(
    private val mailboxRouteRepository: MailboxRouteRepository,
    private val getContact: GetContactUseCase,
    private val packetCodec: PacketCodec,
    private val transportPayloadFactory: OutgoingTransportPayloadFactory,
    private val transportPayloadCodec: TransportPayloadCodec
) : DecoySealer {
    override suspend fun seal(
        contactId: String,
        createdAtEpochMilliseconds: Long
    ): FederatedEnvelope? {
        val route =
            mailboxRouteRepository
                .remoteForContact(contactId)
                .getOrThrow()
                ?.takeIf { route -> route.expiresAtEpochMilliseconds > createdAtEpochMilliseconds }
                ?: return null
        val contact = getContact(contactId).getOrThrow() ?: return null
        val packet = CoverPacket(packetId = IdGenerator.generate())
        val payload = transportPayloadFactory.create(packetCodec.encode(packet).getOrThrow(), packet, contact).getOrNull() ?: return null
        return FederatedEnvelope(
            envelopeId = IdGenerator.generate(),
            senderRoutingId = FederatedEnvelope.HIDDEN_ROUTING_ID,
            recipientDeviceRoutingId = FederatedEnvelope.HIDDEN_ROUTING_ID,
            mailboxRoute = route,
            encryptedPayload = transportPayloadCodec.encode(payload),
            createdAtEpochMilliseconds = createdAtEpochMilliseconds,
            // The same lifetime as a message, so the expiry does not give a decoy away.
            expiresAtEpochMilliseconds = minOf(createdAtEpochMilliseconds + ENVELOPE_LIFETIME_MILLISECONDS, route.expiresAtEpochMilliseconds)
        )
    }

    private companion object {
        const val ENVELOPE_LIFETIME_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
    }
}
