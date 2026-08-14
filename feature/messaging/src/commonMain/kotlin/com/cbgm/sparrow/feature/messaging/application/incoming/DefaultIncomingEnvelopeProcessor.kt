package com.cbgm.sparrow.feature.messaging.application.incoming

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.handler.IncomingMessageHandler
import com.cbgm.sparrow.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.sparrow.feature.messaging.application.routing.ContactByRoutingIdResolver

class DefaultIncomingEnvelopeProcessor(
    private val contactByRoutingIdResolver: ContactByRoutingIdResolver,
    private val localEncryptionKeyPairProvider: LocalEncryptionKeyPairProvider,
    private val incomingMessageHandler: IncomingMessageHandler
) : IncomingEnvelopeProcessor {
    private val logger = SparrowLog.withTag("DefaultIncomingEnvelopeProcessor")

    override suspend fun process(
        envelopeId: String,
        senderRoutingId: String,
        encodedTransportPayload: String
    ): Result<IncomingEnvelopeProcessingResult> =
        runCatching {
            val contactId =
                contactByRoutingIdResolver
                    .resolveContactId(
                        routingId = senderRoutingId
                    ).getOrThrow()
                    ?: run {
                        logger.warn {
                            "Incoming envelope ignored: unknown sender $senderRoutingId"
                        }

                        return@runCatching IncomingEnvelopeProcessingResult.UnknownSender
                    }

            val keyPair =
                localEncryptionKeyPairProvider
                    .getEncryptionKeyPair()
                    .getOrThrow()

            incomingMessageHandler.handle(
                contactId = contactId,
                encodedTransportPayload = encodedTransportPayload,
                localEncryptionPublicKey = keyPair.publicKey,
                localEncryptionPrivateKey = keyPair.privateKey
            )

            contactByRoutingIdResolver
                .reconcileKnownContacts()
                .onFailure { error ->
                    logger.warn {
                        "Contact routing reconciliation failed after envelope $envelopeId: ${error.message}"
                    }
                }

            logger.debug {
                "Incoming envelope stored: envelopeId=$envelopeId, contactId=$contactId"
            }

            IncomingEnvelopeProcessingResult.Processed
        }
}
