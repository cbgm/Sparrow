package com.cbgm.sparrow.feature.conversationorchestration.runtime.incoming

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.handler.IncomingMessageHandler
import com.cbgm.sparrow.core.protocol.handler.IncomingMessageRejectedException
import com.cbgm.sparrow.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.usecase.ReconcileContactTransportRoutingUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ResolveContactIdByRoutingIdUseCase
import com.cbgm.sparrow.feature.conversationorchestration.runtime.routing.GroupRoutingResolver
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessingResult
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessor

class DefaultIncomingEnvelopeProcessor(
    private val resolveContactIdByRoutingId: ResolveContactIdByRoutingIdUseCase,
    private val groupRoutingResolver: GroupRoutingResolver,
    private val reconcileContactTransportRouting: ReconcileContactTransportRoutingUseCase,
    private val localEncryptionKeyPairProvider: LocalEncryptionKeyPairProvider,
    private val incomingMessageHandler: IncomingMessageHandler
) : IncomingEnvelopeProcessor {
    private val logger = SparrowLog.withTag("DefaultIncomingEnvelopeProcessor")

    override suspend fun process(
        envelopeId: String,
        senderRoutingId: String,
        encodedTransportPayload: String
    ): Result<IncomingEnvelopeProcessingResult> =
        safeSuspendCall {
            val contactId =
                resolveContactIdByRoutingId(senderRoutingId)
                    ?: groupRoutingResolver.resolveContactId(senderRoutingId)
                    ?: run {
                        logger.warn {
                            "Incoming envelope ignored: unknown sender $senderRoutingId"
                        }
                        return@safeSuspendCall IncomingEnvelopeProcessingResult.UnknownSender
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
                logger.error(error) {
                    "Incoming envelope rejected permanently: envelopeId=$envelopeId"
                }
                return@safeSuspendCall IncomingEnvelopeProcessingResult.Rejected
            }

            try {
                reconcileContactTransportRouting()
            } catch (error: Throwable) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                logger.error(error) {
                    "Contact routing reconciliation failed after envelope $envelopeId"
                }
            }

            logger.debug {
                "Incoming envelope stored: envelopeId=$envelopeId, contactId=$contactId"
            }

            IncomingEnvelopeProcessingResult.Processed
        }
}
