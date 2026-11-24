package com.cbgm.sparrow.feature.transport.data.controlplane

import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpoint
import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneConfigurationRepository
import kotlinx.coroutines.flow.StateFlow

internal class ControlPlaneConfigurationRepositoryImpl(
    private val localDataSource: ControlPlaneLocalDataSource
) : ControlPlaneConfigurationRepository {
    override val endpoints: StateFlow<List<ControlPlaneEndpoint>> = localDataSource.endpoints
    override val activeEndpoint: StateFlow<ControlPlaneEndpoint?> = localDataSource.activeEndpoint
    override val manualBaseUrls: StateFlow<Set<String>> = localDataSource.manualBaseUrls
    override val directoryBaseUrls: StateFlow<Set<String>> = localDataSource.directoryBaseUrls
    override val directoryUrl: StateFlow<String?> = localDataSource.directoryUrl
    override val jsonDirectoryUrl: StateFlow<String?> = localDataSource.jsonDirectoryUrl
    override val jsonDirectoryBaseUrls: StateFlow<Set<String>> = localDataSource.jsonDirectoryBaseUrls

    override suspend fun initialize() = localDataSource.initialize()

    override suspend fun addManual(baseUrl: String): Result<Unit> = localDataSource.addManual(baseUrl)

    override suspend fun removeManual(baseUrl: String): Result<Unit> = localDataSource.removeManual(baseUrl)

    override suspend fun setDirectoryUrl(url: String?): Result<Unit> = localDataSource.setDirectoryUrl(url)

    override suspend fun useDefaultDirectoryUrlIfUnconfigured(url: String): Result<Unit> =
        localDataSource.useDefaultDirectoryUrlIfUnconfigured(url)

    override suspend fun clearJsonDirectory(): Result<Unit> = localDataSource.clearJsonDirectory()

    override suspend fun clearJsonDirectoryCache(): Result<Unit> = localDataSource.clearJsonDirectoryCache()
}
