package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.policy

import com.cbgm.sparrow.protocol.handler.IncomingPacketContext

/** Transport rules every incoming group verification packet must satisfy. */
object GroupVerificationPacketPolicy {
    private const val MAX_CLOCK_SKEW_MILLISECONDS = 5L * 60L * 1_000L
    private const val SEALED_BOX_TRANSPORT_MODE = "SEALED_BOX"

    fun requireEncryptedTransport(context: IncomingPacketContext) {
        check(context.transportMode == SEALED_BOX_TRANSPORT_MODE) {
            "Group verification packets require encrypted transport"
        }
    }

    fun requireFreshTimestamp(timestamp: Long, receivedAtEpochMilliseconds: Long) {
        require(timestamp <= receivedAtEpochMilliseconds + MAX_CLOCK_SKEW_MILLISECONDS) {
            "Group verification packet was created too far in the future"
        }
    }
}
