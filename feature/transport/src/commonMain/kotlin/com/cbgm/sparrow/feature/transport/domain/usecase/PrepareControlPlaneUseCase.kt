package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneUnavailableException
import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneConfigurationRepository
import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneDirectoryRepository

/**
 * Makes the control planes usable in a process the app's startup never ran in, e.g. one Android started
 * for a push while the app was closed. Control planes from the signed directory live only in its verified
 * cache, so loading the saved configuration alone leaves no control plane. The directory is fetched only
 * when the cache gives none.
 */
class PrepareControlPlaneUseCase(
    private val configuration: ControlPlaneConfigurationRepository,
    private val directorySynchronizer: ControlPlaneDirectoryRepository
) {
    private val logger = SparrowLog.withTag("PrepareControlPlaneUseCase")

    suspend operator fun invoke(): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            configuration.initialize()
            directorySynchronizer.restoreCached().onFailure { error ->
                logger.warn { "Verified control-plane directory could not be restored: ${error.message}" }
            }
            if (configuration.endpoints.value.isEmpty()) {
                directorySynchronizer.refresh().getOrElse { error ->
                    throw ControlPlaneUnavailableException("Control-plane directory could not be loaded", error)
                }
            }
            if (configuration.endpoints.value.isEmpty()) {
                throw ControlPlaneUnavailableException("No control plane is configured")
            }
        }
}
