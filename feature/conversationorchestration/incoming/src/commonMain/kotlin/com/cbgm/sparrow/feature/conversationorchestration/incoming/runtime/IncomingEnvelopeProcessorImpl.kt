package com.cbgm.sparrow.feature.conversationorchestration.incoming.runtime

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.usecase.ReconcileContactTransportRoutingUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ResolveContactIdByRoutingIdUseCase
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.routing.GroupRoutingResolver
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.error.IncomingPacketDeferredException
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.IncomingEnvelopeHandling
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.ParkedEnvelope
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.repository.ParkedEnvelopeRepository
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.ReplayParkedEnvelopesUseCase
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessingResult
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessor
import com.cbgm.sparrow.protocol.handler.IncomingMessageHandler
import com.cbgm.sparrow.protocol.handler.IncomingMessageRejectedException
import com.cbgm.sparrow.protocol.identity.LocalEncryptionKeyPairProvider

/**
 * Processes envelopes of every transport (live, push store, mailboxes). An envelope whose packet arrived too
 * early ([IncomingPacketDeferredException]) is parked and acknowledged: a live envelope that is not
 * acknowledged is lost, a mailbox envelope would block its mailbox until it expires. After every envelope that
 * was handled, the parked envelopes whose wait it may have met are replayed through the same path. Replays run
 * here, outside every workflow, so they never wait for a workflow lock the triggering envelope still holds.
 */
@Suppress("LongParameterList")
internal class IncomingEnvelopeProcessorImpl(
    private val resolveContactIdByRoutingId: ResolveContactIdByRoutingIdUseCase,
    private val groupRoutingResolver: GroupRoutingResolver,
    private val reconcileContactTransportRouting: ReconcileContactTransportRoutingUseCase,
    private val localEncryptionKeyPairProvider: LocalEncryptionKeyPairProvider,
    private val incomingMessageHandler: IncomingMessageHandler,
    private val parkedEnvelopes: ParkedEnvelopeRepository,
    private val replayParkedEnvelopes: ReplayParkedEnvelopesUseCase
) : IncomingEnvelopeProcessor {
    private val logger = SparrowLog.withTag("IncomingEnvelopeProcessorImpl")

    override suspend fun process(
        envelopeId: String,
        senderRoutingId: String,
        encodedTransportPayload: String
    ): Result<IncomingEnvelopeProcessingResult> =
        safeSuspendCall {
            val handling = handle(envelopeId, senderRoutingId, encodedTransportPayload)
            if (handling == IncomingEnvelopeHandling.HANDLED) replayParked()
            when (handling) {
                IncomingEnvelopeHandling.HANDLED,
                IncomingEnvelopeHandling.DEFERRED -> IncomingEnvelopeProcessingResult.Processed
                IncomingEnvelopeHandling.REJECTED -> IncomingEnvelopeProcessingResult.Rejected
                IncomingEnvelopeHandling.UNKNOWN_SENDER -> IncomingEnvelopeProcessingResult.UnknownSender
            }
        }

    private suspend fun handle(
        envelopeId: String,
        senderRoutingId: String,
        encodedTransportPayload: String
    ): IncomingEnvelopeHandling {
        val contactId =
            resolveContactIdByRoutingId(senderRoutingId).getOrThrow()
                ?: groupRoutingResolver.resolveContactId(senderRoutingId)
                ?: run {
                    logger.warn {
                        "Incoming envelope ignored: unknown sender $senderRoutingId"
                    }
                    return IncomingEnvelopeHandling.UNKNOWN_SENDER
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
            return IncomingEnvelopeHandling.REJECTED
        } catch (deferred: IncomingPacketDeferredException) {
            parkedEnvelopes.park(envelopeId, senderRoutingId, encodedTransportPayload, deferred.wait).getOrThrow()
            logger.info {
                "Incoming envelope parked: envelopeId=$envelopeId, waiting for ${deferred.wait.key}"
            }
            return IncomingEnvelopeHandling.DEFERRED
        }

        reconcileContactTransportRouting().onFailure { error ->
            logger.error(error) {
                "Contact routing reconciliation failed after envelope $envelopeId"
            }
        }

        logger.debug {
            "Incoming envelope stored: envelopeId=$envelopeId, contactId=$contactId"
        }

        return IncomingEnvelopeHandling.HANDLED
    }

    /** The triggering envelope is already handled; a failing replay only delays the parked ones. */
    private suspend fun replayParked() {
        replayParkedEnvelopes { parked: ParkedEnvelope ->
            handle(parked.envelopeId, parked.senderRoutingId, parked.encodedTransportPayload)
        }.onFailure { error ->
            logger.error(error) { "Parked envelopes could not be replayed" }
        }
    }
}
