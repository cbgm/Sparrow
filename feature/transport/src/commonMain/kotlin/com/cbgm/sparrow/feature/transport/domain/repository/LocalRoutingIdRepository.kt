package com.cbgm.sparrow.feature.transport.domain.repository

interface LocalRoutingIdRepository {
    suspend fun getLocalRoutingId(): Result<String>
}
