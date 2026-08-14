package com.cbgm.sparrow.feature.transport.routing

interface LocalBootstrapRoutingIdProvider {
    suspend fun getLocalBootstrapRoutingId(): Result<String>
}
