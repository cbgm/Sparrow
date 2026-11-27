package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneDirectoryRepository

/** Fetches and verifies the signed directory at the given url; the result is the number of Control Planes in it. */
class SynchronizeControlPlaneDirectoryUseCase(
    private val synchronizer: ControlPlaneDirectoryRepository
) {
    suspend operator fun invoke(url: String): Result<Int> = synchronizer.synchronizeFrom(url)
}
