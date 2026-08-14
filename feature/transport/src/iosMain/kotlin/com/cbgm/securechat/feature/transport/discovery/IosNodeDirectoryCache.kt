package com.cbgm.securechat.feature.transport.discovery

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

internal class IosNodeDirectoryCache(
    private val json: Json
) : NodeDirectoryCache {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    override suspend fun read(): CachedNodeDirectory? =
        userDefaults.stringForKey(DIRECTORY_KEY)?.let { encoded ->
            runCatching { json.decodeFromString<CachedNodeDirectory>(encoded) }.getOrNull()
        }

    override suspend fun write(directory: CachedNodeDirectory) {
        userDefaults.setObject(json.encodeToString(directory), forKey = DIRECTORY_KEY)
    }

    private companion object {
        const val DIRECTORY_KEY = "securechat.signedNodeDirectory"
    }
}
