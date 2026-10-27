package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model

/** How processing an envelope ended; every case except a failure lets the transport acknowledge it. */
enum class IncomingEnvelopeHandling {
    HANDLED,

    /** Arrived too early and was parked. */
    DEFERRED,

    REJECTED,

    UNKNOWN_SENDER
}
