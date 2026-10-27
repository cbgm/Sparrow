package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.repository

import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.DeferredPacketWait
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.ParkedEnvelope

/**
 * Envelopes that arrived too early ([DeferredPacketWait]). They were acknowledged, so this is their only copy.
 * They are dropped after [RETENTION_MILLISECONDS], the lifetime of an undelivered mailbox envelope.
 */
interface ParkedEnvelopeRepository {
    /**
     * Parks the envelope until [wait] is met. An envelope that is already parked (a copy from another
     * transport, or a replay that has to wait again) keeps its place in the arrival order and its age, and
     * waits for [wait] from now on.
     */
    suspend fun park(
        envelopeId: String,
        senderRoutingId: String,
        encodedTransportPayload: String,
        wait: DeferredPacketWait
    ): Result<Unit>

    /** Every wait at least one parked envelope waits for. */
    suspend fun findWaits(): Result<List<DeferredPacketWait>>

    /** The envelopes waiting for one of [waits], in the order they arrived. */
    suspend fun findWaitingFor(waits: List<DeferredPacketWait>): Result<List<ParkedEnvelope>>

    suspend fun remove(envelopeId: String): Result<Unit>

    companion object {
        const val RETENTION_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
    }
}
