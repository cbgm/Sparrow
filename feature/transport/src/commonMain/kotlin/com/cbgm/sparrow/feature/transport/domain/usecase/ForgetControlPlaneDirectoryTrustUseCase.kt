package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.gateway.ControlPlaneDirectorySynchronizer

/** Forgets the pinned key of the removed directory; only an explicit removal does this. */
class ForgetControlPlaneDirectoryTrustUseCase(
    private val synchronizer: ControlPlaneDirectorySynchronizer
) {
    suspend operator fun invoke(): Result<Unit> = synchronizer.forgetDirectoryTrustOnUserRemoval()
}
