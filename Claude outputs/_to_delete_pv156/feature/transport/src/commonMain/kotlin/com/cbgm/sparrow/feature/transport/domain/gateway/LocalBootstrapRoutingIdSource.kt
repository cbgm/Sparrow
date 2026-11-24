package com.cbgm.sparrow.feature.transport.domain.gateway

interface LocalBootstrapRoutingIdSource {
    suspend fun getLocalBootstrapRoutingId(): Result<String>
}
