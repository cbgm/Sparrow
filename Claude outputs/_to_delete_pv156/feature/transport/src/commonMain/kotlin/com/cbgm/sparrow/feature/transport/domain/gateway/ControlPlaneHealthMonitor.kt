package com.cbgm.sparrow.feature.transport.domain.gateway

interface ControlPlaneHealthMonitor {
    suspend fun refresh()
}
