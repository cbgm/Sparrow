package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.usecase.ResolveContactTransportRoutingIdUseCase
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessingResult
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessor
import com.cbgm.sparrow.feature.transport.circuit.isWaitingForRoute
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.mailbox.InvalidInvitationLetterException
import com.cbgm.sparrow.feature.transport.mailbox.InvitationLetters
import com.cbgm.sparrow.feature.transport.mailbox.InvitationMailboxRenewal
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.feature.transport.mailbox.MailboxNodeUnavailableException
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Processes the envelopes waiting in every local mailbox, including retiring ones a renewal replaced: they keep
 * receiving until the contact learned the new route, and are revoked only after they expired. The device's invitation
 * mailboxes ([InvitationMailboxRenewal]) hold letters of senders without a mailbox route of this device; a letter whose
 * sender its key does not prove is acknowledged and dropped ([InvitationLetters.open]).
 *
 * An envelope that fails stays in its mailbox and is retried on the next sync; it never stops the envelopes
 * behind it or the other mailboxes. One sync runs at a time, so an envelope is never processed twice at once.
 */
class MailboxPendingSynchronizer(
    private val mailboxGateway: MailboxGateway,
    private val mailboxRouteRepository: MailboxRouteRepository,
    private val incomingEnvelopeProcessor: IncomingEnvelopeProcessor,
    private val resolveContactRoutingId: ResolveContactTransportRoutingIdUseCase,
    private val invitationMailbox: InvitationMailboxRenewal,
    private val invitationLetters: InvitationLetters
) {
    private val logger = SparrowLog.withTag("MailboxPendingSynchronizer")
    private val synchronizing = Mutex()

    suspend fun synchronize(): Result<Int> =
        safeSuspendCall(logFailure = false) {
            synchronizing.withLock {
                val credentials = mailboxRouteRepository.receiving().getOrThrow() + invitationMailbox.receiving().getOrThrow()
                credentials.sumOf { credential -> synchronize(credential) }
            }
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
        if (credential.contactId == InvitationMailboxRenewal.CONTACT_ID) return envelopes.count { envelope -> processLetter(credential, envelope) }
        // The mailbox belongs to one contact, and only that contact holds its send capability: the sender is that
        // contact, whatever routing ID the envelope names (envelopes put in by the app itself name none).
        val sender =
            resolveContactRoutingId(credential.contactId).getOrElse { error ->
                logger.warn(error) { "Mailbox of contact ${credential.contactId} not read: the contact's routing ID is unknown" }
                return 0
            }
        return envelopes.count { envelope -> process(credential, sender, envelope.envelopeId, envelope.encryptedPayload) }
    }

    /** True when the letter was opened, processed and acknowledged. */
    private suspend fun processLetter(
        credential: LocalMailboxCredential,
        envelope: FederatedEnvelope
    ): Boolean {
        val letter =
            invitationLetters.open(envelope).getOrElse { failure ->
                when (failure) {
                    is InvalidInvitationLetterException -> dropLetter(credential, envelope, failure)
                    else -> logStillPending(envelope, failure)
                }
                return false
            }
        return process(credential, letter.senderRoutingId, envelope.envelopeId, letter.payload)
    }

    private suspend fun dropLetter(
        credential: LocalMailboxCredential,
        envelope: FederatedEnvelope,
        reason: InvalidInvitationLetterException
    ) {
        mailboxGateway
            .acknowledge(credential, envelope.envelopeId)
            .onSuccess { logger.warn { "Invitation letter ${envelope.envelopeId} dropped: ${reason.message}" } }
            .onFailure { failure -> logStillPending(envelope, failure) }
    }

    /** True when the envelope was processed and acknowledged. */
    private suspend fun process(
        credential: LocalMailboxCredential,
        sender: String,
        envelopeId: String,
        encodedTransportPayload: String
    ): Boolean =
        safeSuspendCall(logFailure = false) {
            when (
                incomingEnvelopeProcessor
                    .process(
                        envelopeId = envelopeId,
                        senderRoutingId = sender,
                        encodedTransportPayload = encodedTransportPayload
                    ).getOrThrow()
            ) {
                IncomingEnvelopeProcessingResult.Processed -> {
                    mailboxGateway.acknowledge(credential, envelopeId).getOrThrow()
                    true
                }

                IncomingEnvelopeProcessingResult.Rejected -> {
                    mailboxGateway.acknowledge(credential, envelopeId).getOrThrow()
                    logger.warn {
                        "Rejected mailbox envelope acknowledged and discarded: envelopeId=$envelopeId"
                    }
                    false
                }

                IncomingEnvelopeProcessingResult.UnknownSender -> false
            }
        }.getOrElse { error ->
            logStillPending(envelopeId, error)
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
    ) = logStillPending(envelope.envelopeId, error)

    private fun logStillPending(
        envelopeId: String,
        error: Throwable
    ) {
        when {
            error.isWaitingForRoute() ->
                logger.info { "Envelope $envelopeId stays pending: waiting for a circuit through enough hops" }

            error is MailboxNodeUnavailableException ->
                logger.info { "Mailbox node is unavailable; envelope $envelopeId stays pending" }

            else -> logger.warn(error) { "Mailbox envelope $envelopeId stays pending for the next sync" }
        }
    }
}
