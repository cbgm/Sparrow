package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.gateway.ControlPlaneConfiguration

/** Removes a Control Plane the user entered. */
class RemoveManualControlPlaneUseCase(
    private val configuration: ControlPlaneConfiguration
) {
    suspend operator fun invoke(baseUrl: String): Result<Unit> = configuration.removeManual(baseUrl)
}
