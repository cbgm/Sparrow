package com.cbgm.securechat.feature.transport.push

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PushDeviceRegistrationRequest(
    @SerialName("relayId")
    val routingId: String,
    val token: String,
    val platform: String
)
