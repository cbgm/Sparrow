package com.cbgm.sparrow.feature.transport.domain.gateway

interface LocalRoutingIdSource {
    suspend fun getLocalRoutingId(): Result<String>
}
