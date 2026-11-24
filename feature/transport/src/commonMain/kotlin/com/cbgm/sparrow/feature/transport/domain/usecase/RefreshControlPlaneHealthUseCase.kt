package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.gateway.ControlPlaneHealthMonitor

/** Asks every configured Control Plane whether it answers. */
class RefreshControlPlaneHealthUseCase(
    private val healthMonitor: ControlPlaneHealthMonitor
) {
    suspend operator fun invoke() = healthMonitor.refresh()
}
