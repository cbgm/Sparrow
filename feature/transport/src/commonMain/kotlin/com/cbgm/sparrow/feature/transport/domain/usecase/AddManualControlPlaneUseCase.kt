package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneConfigurationRepository

/** Adds a Control Plane the user entered. */
class AddManualControlPlaneUseCase(
    private val configuration: ControlPlaneConfigurationRepository
) {
    suspend operator fun invoke(baseUrl: String): Result<Unit> = configuration.addManual(baseUrl)
}
