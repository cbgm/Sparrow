package com.cbgm.sparrow.feature.messaging.runtime.incoming

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.handler.IncomingMessageHandler
import com.cbgm.sparrow.core.protocol.handler.IncomingMessageRejectedException
import com.cbgm.sparrow.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.sparrow.feature.messaging.data.datasource.ContactByRoutingIdDataSource
import com.cbgm.sparrow.feature.messaging.data.datasource.ContactRoutingReconciliationDataSource

class DefaultIncomingEnvelopeProcessor(
    private val contactByRoutingIdDataSource: ContactByRoutingIdDataSource,
    private val contactRoutingReconciliationDataSource: ContactRoutingReconciliationDataSource,
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
                contactByRoutingIdDataSource
                    .resolveContactId(senderRoutingId)
                    .getOrThrow()
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

            try {
                incomingMessageHandler.handle(
                    contactId = contactId,
                    encodedTransportPayload = encodedTransportPayload,
                    localEncryptionPublicKey = keyPair.publicKey,
                    localEncryptionPrivateKey = keyPair.privateKey
                )
            } catch (error: IncomingMessageRejectedException) {
                logger.warn {
                    "Incoming envelope rejected permanently: envelopeId=$envelopeId, reason=${error.message}"
                }
                return@runCatching IncomingEnvelopeProcessingResult.Rejected
            }

            contactRoutingReconciliationDataSource
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
