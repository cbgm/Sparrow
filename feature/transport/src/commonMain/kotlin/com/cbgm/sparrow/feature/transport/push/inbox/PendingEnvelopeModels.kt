package com.cbgm.sparrow.feature.transport.push.inbox

import com.cbgm.sparrow.feature.transport.gateway.model.TransportEnvelope
import kotlinx.serialization.Serializable

@Serializable
data class PendingTransportEnvelopesResponse(
    val envelopes: List<TransportEnvelope>
)
