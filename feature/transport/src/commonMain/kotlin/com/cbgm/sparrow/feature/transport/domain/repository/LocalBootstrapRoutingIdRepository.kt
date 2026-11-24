package com.cbgm.sparrow.feature.transport.domain.repository

interface LocalBootstrapRoutingIdRepository {
    suspend fun getLocalBootstrapRoutingId(): Result<String>
}
