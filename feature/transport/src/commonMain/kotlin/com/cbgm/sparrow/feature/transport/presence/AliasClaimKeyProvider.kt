package com.cbgm.sparrow.feature.transport.presence

import com.cbgm.sparrow.feature.transport.discovery.NodeDirectoryCache
import com.cbgm.sparrow.feature.transport.discovery.SignedNodeDirectory
import kotlinx.serialization.json.Json

/**
 * The presence directory's alias claim key from the last node directory that was verified and cached. Null until a
 * directory carries one; the device then registers without phone aliases.
 */
class AliasClaimKeyProvider(
    private val cache: NodeDirectoryCache,
    private val json: Json
) {
    suspend fun current(): ByteArray? =
        cache.read()?.let { cached ->
            runCatching { json.decodeFromString<SignedNodeDirectory>(cached.encodedDirectory) }.getOrNull()
        }?.directory?.aliasClaimKey
}
