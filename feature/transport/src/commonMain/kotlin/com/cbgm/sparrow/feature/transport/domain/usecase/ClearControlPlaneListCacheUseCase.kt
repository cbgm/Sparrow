package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.gateway.ControlPlaneConfiguration

/** Forgets the addresses the legacy JSON list gave. */
class ClearControlPlaneListCacheUseCase(
    private val configuration: ControlPlaneConfiguration
) {
    suspend operator fun invoke(): Result<Unit> = configuration.clearJsonDirectoryCache()
}
