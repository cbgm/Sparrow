package com.cbgm.sparrow.feature.messaging.application.incoming

enum class IncomingEnvelopeProcessingResult {
    Processed,
    UnknownSender
}

interface IncomingEnvelopeProcessor {
    suspend fun process(
        envelopeId: String,
        senderRoutingId: String,
        encodedTransportPayload: String
    ): Result<IncomingEnvelopeProcessingResult>
}
