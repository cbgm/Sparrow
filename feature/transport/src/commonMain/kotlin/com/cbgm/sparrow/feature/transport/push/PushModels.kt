package com.cbgm.sparrow.feature.transport.push

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PushDeviceRegistrationRequest(
    @SerialName("routingId")
    val routingId: String,
    val token: String,
    val platform: String
)
