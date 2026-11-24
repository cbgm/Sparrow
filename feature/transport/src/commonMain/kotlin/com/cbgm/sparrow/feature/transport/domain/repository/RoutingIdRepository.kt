package com.cbgm.sparrow.feature.transport.domain.repository

interface RoutingIdRepository {
    suspend fun getLocalRoutingId(): Result<String>

    suspend fun getLocalBootstrapRoutingId(): Result<String>
}
