package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model

/** An acknowledged envelope that waits for [wait] before it is processed again, as it arrived. */
data class ParkedEnvelope(
    val envelopeId: String,
    val senderRoutingId: String,
    val encodedTransportPayload: String,
    val wait: DeferredPacketWait,
    val parkedAtEpochMilliseconds: Long
)
