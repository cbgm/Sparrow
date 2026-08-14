package com.cbgm.securechat.feature.transport.routing

interface LocalBootstrapRoutingIdProvider {
    suspend fun getLocalBootstrapRoutingId(): Result<String>
}
