package com.cbgm.securechat.feature.transport.push.inbox

import com.cbgm.securechat.feature.transport.gateway.model.TransportEnvelope
import kotlinx.serialization.Serializable

@Serializable
data class PendingTransportEnvelopesResponse(
    val envelopes: List<TransportEnvelope>
)
