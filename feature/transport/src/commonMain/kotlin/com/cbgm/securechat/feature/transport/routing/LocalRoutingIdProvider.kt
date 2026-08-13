package com.cbgm.securechat.feature.transport.routing

interface LocalRoutingIdProvider {
    suspend fun getLocalRoutingId(): Result<String>
}
