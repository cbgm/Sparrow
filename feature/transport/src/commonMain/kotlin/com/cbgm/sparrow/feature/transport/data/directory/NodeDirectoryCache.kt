package com.cbgm.sparrow.feature.transport.data.directory

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal interface NodeDirectoryCache {
    suspend fun read(): CachedNodeDirectoryDto?

    suspend fun write(directory: CachedNodeDirectoryDto)
}

@Serializable
internal data class CachedNodeDirectoryDto(
    val encodedDirectory: String,
    @SerialName("trustedAuthorityNodeId")
    val trustedRootNodeId: String,
    val sourceControlPlaneBaseUrl: String? = null,
    val trustedRootsByControlPlane: Map<String, String> = emptyMap()
) {
    fun trustedRootForSource(): String =
        sourceControlPlaneBaseUrl
            ?.let(trustedRootsByControlPlane::get)
            ?: trustedRootNodeId

    fun trustedRootFor(baseUrl: String): String? =
        trustedRootsByControlPlane[baseUrl]
}
