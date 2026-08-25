package com.cbgm.sparrow.feature.messaging.runtime.incoming

import com.cbgm.sparrow.core.logging.SparrowLog
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
    private val logger = SparrowLog.withTag("DefaultIncomingEnvelopeRunner")

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
                IncomingEnvelopeProcessingResult.Processed ->
                    acknowledgeEnvelope(
                        envelopeId = envelopeId,
                        rejected = false
                    )

                IncomingEnvelopeProcessingResult.Rejected ->
                    acknowledgeEnvelope(
                        envelopeId = envelopeId,
                        rejected = true
                    )

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

    private suspend fun acknowledgeEnvelope(
        envelopeId: String,
        rejected: Boolean
    ) {
        incomingEnvelopeGateway
            .acknowledge(
                envelopeId = envelopeId
            ).getOrThrow()

        if (rejected) {
            logger.warn {
                "Rejected incoming envelope acknowledged and discarded: envelopeId=$envelopeId"
            }
        } else {
            logger.debug {
                "Incoming envelope acknowledged: envelopeId=$envelopeId"
            }
        }
    }
}
