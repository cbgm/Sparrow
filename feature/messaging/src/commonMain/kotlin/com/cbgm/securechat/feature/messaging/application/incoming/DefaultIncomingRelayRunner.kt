package com.cbgm.securechat.feature.messaging.application.incoming

import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.feature.messaging.application.relay.IncomingRelayGateway
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DefaultIncomingRelayRunner(
    private val incomingRelayGateway: IncomingRelayGateway,
    private val incomingEnvelopeProcessor: IncomingEnvelopeProcessor
) : IncomingRelayRunner {
    private val logger = SecureChatLog.withTag("DefaultIncomingRelayRunner")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var collectionJob: Job? = null

    override fun start() {
        if (collectionJob?.isActive == true) {
            return
        }

        collectionJob =
            scope.launch {
                incomingRelayGateway
                    .incomingEnvelopes
                    .collect { envelope ->
                        processEnvelope(
                            envelopeId = envelope.envelopeId,
                            senderRelayId = envelope.senderRelayId,
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
        senderRelayId: String,
        encodedTransportPayload: String
    ) {
        try {
            when (
                incomingEnvelopeProcessor
                    .process(
                        envelopeId = envelopeId,
                        senderRelayId = senderRelayId,
                        encodedTransportPayload = encodedTransportPayload
                    ).getOrThrow()
            ) {
                IncomingEnvelopeProcessingResult.Processed -> {
                    incomingRelayGateway
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
