package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneConfigurationRepository

/** Sets the signed directory the Control Planes come from; null removes it. */
class SetControlPlaneDirectoryUseCase(
    private val configuration: ControlPlaneConfigurationRepository
) {
    suspend operator fun invoke(url: String?): Result<Unit> = configuration.setDirectoryUrl(url)
}
