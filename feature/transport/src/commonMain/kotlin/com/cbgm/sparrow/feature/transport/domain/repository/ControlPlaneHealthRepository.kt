package com.cbgm.sparrow.feature.transport.domain.repository

interface ControlPlaneHealthRepository {
    suspend fun refresh()
}
