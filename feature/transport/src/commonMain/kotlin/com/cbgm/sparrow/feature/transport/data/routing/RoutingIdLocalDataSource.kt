package com.cbgm.sparrow.feature.transport.data.routing

internal interface RoutingIdLocalDataSource {
    suspend fun getLocalRoutingId(): Result<String>

    suspend fun getLocalBootstrapRoutingId(): Result<String>
}
