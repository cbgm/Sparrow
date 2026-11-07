package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.usecase.ResolveContactTransportRoutingIdUseCase
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessingResult
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessor
import com.cbgm.sparrow.feature.transport.circuit.isWaitingForRoute
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.feature.transport.mailbox.MailboxNodeUnavailableException
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Processes the envelopes waiting in every local mailbox, including retiring ones a renewal replaced: they keep
 * receiving until the contact learned the new route, and are revoked only after they expired.
 *
 * An envelope that fails stays in its mailbox and is retried on the next sync; it never stops the envelopes
 * behind it or the other mailboxes. One sync runs at a time, so an envelope is never processed twice at once.
 */
class MailboxPendingSynchronizer(
    private val mailboxGateway: MailboxGateway,
    private val mailboxRouteRepository: MailboxRouteRepository,
    private val incomingEnvelopeProcessor: IncomingEnvelopeProcessor,
    private val resolveContactRoutingId: ResolveContactTransportRoutingIdUseCase
) {
    private val logger = SparrowLog.withTag("MailboxPendingSynchronizer")
    private val synchronizing = Mutex()

    suspend fun synchronize(): Result<Int> =
        safeSuspendCall(logFailure = false) {
            synchronizing.withLock { mailboxRouteRepository.receiving().getOrThrow().sumOf { credential -> synchronize(credential) } }
        }

    /** Only [credentials], the mailboxes known to hold envelopes. */
    suspend fun synchronize(credentials: List<LocalMailboxCredential>): Result<Int> =
        safeSuspendCall(logFailure = false) {
            synchronizing.withLock { credentials.sumOf { credential -> synchronize(credential) } }
        }

    private suspend fun synchronize(credential: LocalMailboxCredential): Int {
        val envelopes =
            mailboxGateway.pending(credential).getOrElse { error ->
                logUnreadable(credential, error)
                return 0
            }
        if (envelopes.isEmpty()) return 0
        // The mailbox belongs to one contact, and only that contact holds its send capability: the sender is that
        // contact, whatever routing ID the envelope names (envelopes put in by the app itself name none).
        val sender =
            resolveContactRoutingId(credential.contactId).getOrElse { error ->
                logger.warn(error) { "Mailbox of contact ${credential.contactId} not read: the contact's routing ID is unknown" }
                return 0
            }
        return envelopes.count { envelope -> process(credential, sender, envelope) }
    }

    /** True when the envelope was processed and acknowledged. */
    private suspend fun process(
        credential: LocalMailboxCredential,
        sender: String,
        envelope: FederatedEnvelope
    ): Boolean =
        safeSuspendCall(logFailure = false) {
            when (
                incomingEnvelopeProcessor
                    .process(
                        envelopeId = envelope.envelopeId,
                        senderRoutingId = sender,
                        encodedTransportPayload = envelope.encryptedPayload
                    ).getOrThrow()
            ) {
                IncomingEnvelopeProcessingResult.Processed -> {
                    mailboxGateway.acknowledge(credential, envelope.envelopeId).getOrThrow()
                    true
                }

                IncomingEnvelopeProcessingResult.Rejected -> {
                    mailboxGateway.acknowledge(credential, envelope.envelopeId).getOrThrow()
                    logger.warn {
                        "Rejected mailbox envelope acknowledged and discarded: envelopeId=${envelope.envelopeId}"
                    }
                    false
                }

                IncomingEnvelopeProcessingResult.UnknownSender -> false
            }
        }.getOrElse { error ->
            logStillPending(envelope, error)
            false
        }

    // A node that is down is expected (it may come back or be replaced), and so is waiting for a protecting circuit
    // the user chose; only other failures are errors.
    private fun logUnreadable(
        credential: LocalMailboxCredential,
        error: Throwable
    ) {
        when {
            error.isWaitingForRoute() ->
                logger.info { "Mailbox of contact ${credential.contactId} not read: waiting for a circuit through enough hops" }

            error is MailboxNodeUnavailableException ->
                logger.info { "Mailbox node of contact ${credential.contactId} is unavailable; retried on the next sync" }

            else -> logger.warn(error) { "Mailbox of contact ${credential.contactId} could not be read" }
        }
    }

    private fun logStillPending(
        envelope: FederatedEnvelope,
        error: Throwable
    ) {
        when {
            error.isWaitingForRoute() ->
                logger.info { "Envelope ${envelope.envelopeId} stays pending: waiting for a circuit through enough hops" }

            error is MailboxNodeUnavailableException ->
                logger.info { "Mailbox node is unavailable; envelope ${envelope.envelopeId} stays pending" }

            else -> logger.warn(error) { "Mailbox envelope ${envelope.envelopeId} stays pending for the next sync" }
        }
    }
}
