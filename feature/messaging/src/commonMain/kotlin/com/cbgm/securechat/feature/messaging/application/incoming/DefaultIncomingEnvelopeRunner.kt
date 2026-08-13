package com.cbgm.securechat.feature.messaging.application.incoming

import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.feature.messaging.application.routing.IncomingEnvelopeGateway
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DefaultIncomingEnvelopeRunner(
    private val incomingEnvelopeGateway: IncomingEnvelopeGateway,
    private val incomingEnvelopeProcessor: IncomingEnvelopeProcessor
) : IncomingEnvelopeRunner {
    private val logger = SecureChatLog.withTag("DefaultIncomingEnvelopeRunner")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var collectionJob: Job? = null

    override fun start() {
        if (collectionJob?.isActive == true) {
            return
        }

        collectionJob =
            scope.launch {
                incomingEnvelopeGateway
                    .incomingEnvelopes
                    .collect { envelope ->
                        processEnvelope(
                            envelopeId = envelope.envelopeId,
                            senderRoutingId = envelope.senderRoutingId,
                            encodedTransportPayload = envelope.encodedTransportPayload
                        )
                    }
            }
    }

    override fun stop() {
        collectionJob?.cancel()
        collectionJob = null
    }

    private suspend fun processEnvelope(
        envelopeId: String,
        senderRoutingId: String,
        encodedTransportPayload: String
    ) {
        try {
            when (
                incomingEnvelopeProcessor
                    .process(
                        envelopeId = envelopeId,
                        senderRoutingId = senderRoutingId,
                        encodedTransportPayload = encodedTransportPayload
                    ).getOrThrow()
            ) {
                IncomingEnvelopeProcessingResult.Processed -> {
                    incomingEnvelopeGateway
                        .acknowledge(
                            envelopeId = envelopeId
                        ).getOrThrow()

                    logger.debug {
                        "Incoming envelope acknowledged: envelopeId=$envelopeId"
                    }
                }

                IncomingEnvelopeProcessingResult.UnknownSender -> Unit
            }
        } catch (
            error: CancellationException
        ) {
            throw error
        } catch (
            error: Throwable
        ) {
            logger.error(error) {
                "Incoming envelope failed: envelopeId=$envelopeId"
            }
        }
    }
}
