package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.gateway.ControlPlaneDirectorySynchronizer

/** Adds the addresses of an unsigned JSON list at the given url as removable manual entries. */
class ImportControlPlaneListUseCase(
    private val synchronizer: ControlPlaneDirectorySynchronizer
) {
    suspend operator fun invoke(url: String): Result<Int> = synchronizer.importJsonDirectory(url)
}
