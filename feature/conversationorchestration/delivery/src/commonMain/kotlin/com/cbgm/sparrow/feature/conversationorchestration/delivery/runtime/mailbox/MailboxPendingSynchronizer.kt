package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessingResult
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessor
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository

/**
 * Processes the envelopes waiting in every local mailbox.
 *
 * An envelope that fails stays in its mailbox and is retried on the next sync; it never stops the envelopes
 * behind it or the other mailboxes.
 */
class MailboxPendingSynchronizer(
    private val mailboxGateway: MailboxGateway,
    private val mailboxRouteRepository: MailboxRouteRepository,
    private val incomingEnvelopeProcessor: IncomingEnvelopeProcessor
) {
    private val logger = SparrowLog.withTag("MailboxPendingSynchronizer")

    suspend fun synchronize(): Result<Int> =
        safeSuspendCall(logFailure = false) {
            mailboxRouteRepository
                .allLocal()
                .getOrThrow()
                .filterNot(LocalMailboxCredential::revocationPending)
                .sumOf { credential -> synchronize(credential) }
        }

    private suspend fun synchronize(credential: LocalMailboxCredential): Int {
        val envelopes =
            mailboxGateway.pending(credential).getOrElse { error ->
                logger.warn(error) { "Mailbox of contact ${credential.contactId} could not be read" }
                return 0
            }
        return envelopes.count { envelope -> process(credential, envelope) }
    }

    /** True when the envelope was processed and acknowledged. */
    private suspend fun process(credential: LocalMailboxCredential, envelope: FederatedEnvelope): Boolean =
        safeSuspendCall(logFailure = false) {
            when (
                incomingEnvelopeProcessor
                    .process(
                        envelopeId = envelope.envelopeId,
                        senderRoutingId = envelope.senderRoutingId,
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
            logger.warn(error) { "Mailbox envelope ${envelope.envelopeId} stays pending for the next sync" }
            false
        }
}
