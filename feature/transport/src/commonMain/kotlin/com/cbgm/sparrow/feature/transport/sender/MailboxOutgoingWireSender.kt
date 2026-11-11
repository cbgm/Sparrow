package com.cbgm.sparrow.feature.transport.sender

import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.transport.config.TransportConfig
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.mailbox.InvitationLetters
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.feature.transport.presence.PhoneAliasBinding
import com.cbgm.sparrow.feature.transport.routing.LocalBootstrapRoutingIdProvider
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository
import com.cbgm.sparrow.protocol.transport.OutgoingWireAcceptance
import com.cbgm.sparrow.protocol.transport.OutgoingWireSender
import kotlinx.coroutines.TimeoutCancellationException

/**
 * Puts every packet into a mailbox of the recipient, through the circuit ([MailboxGateway] uses it), so no node learns
 * who writes to whom: into the recipient's mailbox for this device when its route is known, otherwise into the
 * recipient's invitation mailbox ([InvitationLetters]). A mailbox that cannot be reached fails the send, and the outbox
 * retries it.
 */
internal class MailboxOutgoingWireSender(
    private val localRoutingIdProvider: LocalRoutingIdProvider,
    private val localBootstrapRoutingIdProvider: LocalBootstrapRoutingIdProvider,
    private val transportConfig: TransportConfig,
    private val mailboxRouteRepository: MailboxRouteRepository,
    private val mailboxGateway: MailboxGateway,
    private val invitationLetters: InvitationLetters,
    private val aliasBinding: PhoneAliasBinding
) : OutgoingWireSender {
    private val logger = SparrowLog.withTag("MailboxOutgoingWireSender")

    override suspend fun send(
        recipientAddress: String,
        encodedTransportPayload: String
    ): Result<Unit> =
        sendWithAcceptance(recipientAddress, encodedTransportPayload, recipientContactId = null).map { }

    override suspend fun sendWithAcceptance(
        recipientAddress: String,
        encodedTransportPayload: String,
        recipientContactId: String?
    ): Result<OutgoingWireAcceptance> =
        runCatching {
            require(recipientAddress.isNotBlank()) { "Recipient address must not be blank" }
            require(encodedTransportPayload.isNotBlank()) { "Transport payload must not be blank" }

            val senderRoutingId = senderRoutingId(recipientAddress)
            val envelopeId = IdGenerator.generate()
            val createdAt = SystemClock.nowEpochMilliseconds()
            // By contact when it is known: the routing table holds only the phone alias, not the address messages to
            // a verified contact are sent to, so a lookup by that address would never find the route.
            val route =
                (recipientContactId?.let { mailboxRouteRepository.remoteForContact(it) } ?: mailboxRouteRepository.remoteForRecipientRoutingId(recipientAddress))
                    .getOrThrow()
                    ?.takeIf { it.expiresAtEpochMilliseconds > createdAt }

            val expiresAt =
                when (route) {
                    // Without a mailbox route of the recipient (a first invitation, or before its route arrived), the
                    // packet goes into the recipient's invitation mailbox, through the circuit like any mailbox.
                    null ->
                        invitationLetters
                            .send(senderRoutingId, recipientAddress, encodedTransportPayload, envelopeId, createdAt)
                            .getOrThrow()
                            .also { logger.debug { "Packet put into the recipient's invitation mailbox (no mailbox route of the recipient yet)" } }

                    else -> {
                        val envelope =
                            FederatedEnvelope(
                                envelopeId = envelopeId,
                                senderRoutingId = senderRoutingId,
                                recipientDeviceRoutingId = recipientAddress,
                                mailboxRoute = route,
                                encryptedPayload = encodedTransportPayload,
                                createdAtEpochMilliseconds = createdAt,
                                expiresAtEpochMilliseconds = minOf(createdAt + ENVELOPE_LIFETIME_MILLISECONDS, route.expiresAtEpochMilliseconds)
                            )
                        mailboxGateway.deliver(envelope.withoutRoutingIds()).getOrThrow()
                        logger.debug { "Message put into the contact's mailbox" }
                        envelope.expiresAtEpochMilliseconds
                    }
                }

            OutgoingWireAcceptance(expiresAtEpochMilliseconds = expiresAt)
        }

    /** A first invitation to a phone alias names the own phone alias, once a Control Plane bound it to this device. */
    private suspend fun senderRoutingId(recipientAddress: String): String {
        if (!recipientAddress.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX)) return localRoutingIdProvider.getLocalRoutingId().getOrThrow()
        val alias = localBootstrapRoutingIdProvider.getLocalBootstrapRoutingId().getOrThrow()
        try {
            aliasBinding.awaitBound(alias, transportConfig.acknowledgementTimeoutMilliseconds)
        } catch (timeout: TimeoutCancellationException) {
            logger.warn { "First invite not sent: own phone number is not bound at any Control Plane" }
            throw IllegalStateException("Own phone number is not bound at any Control Plane yet", timeout)
        }
        return alias
    }

    private companion object {
        const val ENVELOPE_LIFETIME_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
        const val BOOTSTRAP_ROUTING_ID_PREFIX = "scphone1_"
    }
}
