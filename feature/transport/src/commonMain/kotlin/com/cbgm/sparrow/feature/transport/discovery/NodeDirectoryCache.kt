package com.cbgm.sparrow.feature.transport.discovery

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface NodeDirectoryCache {
    suspend fun read(): CachedNodeDirectory?

    suspend fun write(directory: CachedNodeDirectory)
}

@Serializable
data class CachedNodeDirectory(
    val encodedDirectory: String,
    @SerialName("trustedAuthorityNodeId")
    val trustedRootNodeId: String
)
