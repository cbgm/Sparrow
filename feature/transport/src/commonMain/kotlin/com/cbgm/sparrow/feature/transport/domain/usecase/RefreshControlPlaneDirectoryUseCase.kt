package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.gateway.ControlPlaneDirectorySynchronizer

/** Fetches the signed directory anew; the result is the number of Control Planes in it. */
class RefreshControlPlaneDirectoryUseCase(
    private val synchronizer: ControlPlaneDirectorySynchronizer
) {
    suspend operator fun invoke(): Result<Int> = synchronizer.refresh()
}
