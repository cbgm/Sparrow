package com.cbgm.sparrow.feature.transport.data.controlplane

import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneDirectoryRepository

internal class ControlPlaneDirectoryRepositoryImpl(
    private val synchronizer: ControlPlaneDirectorySynchronizer
) : ControlPlaneDirectoryRepository {
    override suspend fun restoreCached(): Result<Int> = synchronizer.restoreCached()

    override suspend fun refresh(): Result<Int> = synchronizer.refresh()

    override suspend fun synchronizeFrom(url: String): Result<Int> = synchronizer.synchronizeFrom(url)

    override suspend fun importJsonDirectory(url: String): Result<Int> = synchronizer.importJsonDirectory(url)

    override suspend fun forgetDirectoryTrustOnUserRemoval(): Result<Unit> = synchronizer.forgetDirectoryTrustOnUserRemoval()
}
