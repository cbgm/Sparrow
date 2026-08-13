package com.cbgm.securechat.feature.transport.push.inbox

import com.cbgm.securechat.feature.transport.gateway.model.TransportEnvelope

interface PendingEnvelopeGateway {
    suspend fun getPendingEnvelopes(wakeUpId: String): Result<List<TransportEnvelope>>

    suspend fun acknowledge(
        wakeUpId: String,
        envelopeId: String
    ): Result<Unit>
}
