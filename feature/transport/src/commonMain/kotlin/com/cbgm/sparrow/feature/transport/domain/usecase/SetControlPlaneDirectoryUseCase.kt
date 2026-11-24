package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.gateway.ControlPlaneConfiguration

/** Sets the signed directory the Control Planes come from; null removes it. */
class SetControlPlaneDirectoryUseCase(
    private val configuration: ControlPlaneConfiguration
) {
    suspend operator fun invoke(url: String?): Result<Unit> = configuration.setDirectoryUrl(url)
}
