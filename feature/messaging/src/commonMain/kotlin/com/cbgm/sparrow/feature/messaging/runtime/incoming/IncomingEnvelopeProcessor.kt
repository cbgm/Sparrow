package com.cbgm.sparrow.feature.messaging.runtime.incoming

enum class IncomingEnvelopeProcessingResult {
    Processed,
    Rejected,
    UnknownSender
}

interface IncomingEnvelopeProcessor {
    suspend fun process(
        envelopeId: String,
        senderRoutingId: String,
        encodedTransportPayload: String
    ): Result<IncomingEnvelopeProcessingResult>
}
