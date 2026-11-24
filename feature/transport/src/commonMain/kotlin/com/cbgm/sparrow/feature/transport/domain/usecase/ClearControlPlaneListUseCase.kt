package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.gateway.ControlPlaneConfiguration

/** Removes the legacy JSON list as a source; the addresses it gave stay until replaced. */
class ClearControlPlaneListUseCase(
    private val configuration: ControlPlaneConfiguration
) {
    suspend operator fun invoke(): Result<Unit> = configuration.clearJsonDirectory()
}
