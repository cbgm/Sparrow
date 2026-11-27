package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneStatusRepository

/** Asks every configured Control Plane whether it answers. */
class RefreshControlPlaneHealthUseCase(
    private val statusRepository: ControlPlaneStatusRepository
) {
    suspend operator fun invoke() = statusRepository.refreshHealth()
}
