package com.cbgm.sparrow.feature.messaging.runtime.mailbox

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.core.protocol.mailbox.MailboxRouteRepository
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessingResult
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessor
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway

class MailboxPendingSynchronizer(
    private val mailboxGateway: MailboxGateway,
    private val mailboxRouteRepository: MailboxRouteRepository,
    private val incomingEnvelopeProcessor: IncomingEnvelopeProcessor
) {
    private val logger = SparrowLog.withTag("MailboxPendingSynchronizer")

    suspend fun synchronize(): Result<Int> =
        runCatching {
            var processed = 0
            mailboxRouteRepository
                .allLocal()
                .getOrThrow()
                .filterNot(LocalMailboxCredential::revocationPending)
                .forEach { credential ->
                    mailboxGateway.pending(credential).getOrThrow().forEach { envelope ->
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
                                processed += 1
                            }

                            IncomingEnvelopeProcessingResult.Rejected -> {
                                mailboxGateway.acknowledge(credential, envelope.envelopeId).getOrThrow()
                                logger.warn {
                                    "Rejected mailbox envelope acknowledged and discarded: " +
                                        "envelopeId=${envelope.envelopeId}"
                                }
                            }

                            IncomingEnvelopeProcessingResult.UnknownSender -> Unit
                        }
                    }
                }
            processed
        }
}
