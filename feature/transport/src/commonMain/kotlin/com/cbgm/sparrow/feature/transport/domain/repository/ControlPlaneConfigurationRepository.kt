package com.cbgm.sparrow.feature.transport.domain.repository

import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpoint
import kotlinx.coroutines.flow.StateFlow

/** The Control Planes the user configured, directly or through a signed directory. */
interface ControlPlaneConfigurationRepository {
    val endpoints: StateFlow<List<ControlPlaneEndpoint>>
    val activeEndpoint: StateFlow<ControlPlaneEndpoint?>
    val manualBaseUrls: StateFlow<Set<String>>
    val directoryBaseUrls: StateFlow<Set<String>>
    val directoryUrl: StateFlow<String?>

    /** Explicitly user-subscribed legacy JSON list, never a signed-directory trust source. */
    val jsonDirectoryUrl: StateFlow<String?>
    val jsonDirectoryBaseUrls: StateFlow<Set<String>>

    /** Loads the saved configuration; until then everything is empty. */
    suspend fun initialize()

    suspend fun addManual(baseUrl: String): Result<Unit>

    suspend fun removeManual(baseUrl: String): Result<Unit>

    suspend fun setDirectoryUrl(url: String?): Result<Unit>

    /** Optional development bootstrap, ignored once the user has chosen/removed a directory. */
    suspend fun useDefaultDirectoryUrlIfUnconfigured(url: String): Result<Unit>

    suspend fun clearJsonDirectory(): Result<Unit>

    /** Explicit user removal, not a source URL edit: discard old JSON hints. */
    suspend fun clearJsonDirectoryCache(): Result<Unit>
}
