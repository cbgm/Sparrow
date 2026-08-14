package com.cbgm.sparrow.feature.transport.routing

interface LocalRoutingIdProvider {
    suspend fun getLocalRoutingId(): Result<String>
}
