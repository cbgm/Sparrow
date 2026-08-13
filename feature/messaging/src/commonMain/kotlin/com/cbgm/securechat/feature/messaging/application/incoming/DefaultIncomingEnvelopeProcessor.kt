package com.cbgm.securechat.feature.messaging.application.incoming

import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.protocol.handler.IncomingMessageHandler
import com.cbgm.securechat.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.securechat.feature.messaging.application.relay.ContactByRelayIdResolver

class DefaultIncomingEnvelopeProcessor(
    private val contactByRelayIdResolver: ContactByRelayIdResolver,
    private val localEncryptionKeyPairProvider: LocalEncryptionKeyPairProvider,
    private val incomingMessageHandler: IncomingMessageHandler
) : IncomingEnvelopeProcessor {
    private val logger = SecureChatLog.withTag("DefaultIncomingEnvelopeProcessor")

    override suspend fun process(
        envelopeId: String,
        senderRelayId: String,
        encodedTransportPayload: String
    ): Result<IncomingEnvelopeProcessingResult> =
        runCatching {
            val contactId =
                contactByRelayIdResolver
                    .resolveContactId(
                        relayId = senderRelayId
                    ).getOrThrow()
                    ?: run {
                        logger.warn {
                            "Incoming envelope ignored: unknown sender $senderRelayId"
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

            contactByRelayIdResolver
                .reconcileKnownContacts()
                .onFailure { error ->
                    logger.warn {
                        "Contact relay reconciliation failed after envelope $envelopeId: ${error.message}"
                    }
                }

            logger.debug {
                "Incoming envelope stored: envelopeId=$envelopeId, contactId=$contactId"
            }

            IncomingEnvelopeProcessingResult.Processed
        }
}
