package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneHealthRepository

/** Asks every configured Control Plane whether it answers. */
class RefreshControlPlaneHealthUseCase(
    private val healthMonitor: ControlPlaneHealthRepository
) {
    suspend operator fun invoke() = healthMonitor.refresh()
}
