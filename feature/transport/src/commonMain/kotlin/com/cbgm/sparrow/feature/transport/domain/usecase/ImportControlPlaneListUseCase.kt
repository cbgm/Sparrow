package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneDirectoryRepository

/** Adds the addresses of an unsigned JSON list at the given url as removable manual entries. */
class ImportControlPlaneListUseCase(
    private val synchronizer: ControlPlaneDirectoryRepository
) {
    suspend operator fun invoke(url: String): Result<Int> = synchronizer.importJsonDirectory(url)
}
