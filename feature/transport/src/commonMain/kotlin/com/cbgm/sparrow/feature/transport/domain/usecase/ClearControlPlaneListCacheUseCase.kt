package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneConfigurationRepository

/** Forgets the addresses the legacy JSON list gave. */
class ClearControlPlaneListCacheUseCase(
    private val configuration: ControlPlaneConfigurationRepository
) {
    suspend operator fun invoke(): Result<Unit> = configuration.clearJsonDirectoryCache()
}
