package com.cbgm.securechat.feature.transport.discovery

import android.content.Context
import kotlinx.serialization.json.Json

internal class AndroidNodeDirectoryCache(
    context: Context,
    private val json: Json
) : NodeDirectoryCache {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun read(): CachedNodeDirectory? =
        preferences.getString(DIRECTORY_KEY, null)?.let { encoded ->
            runCatching { json.decodeFromString<CachedNodeDirectory>(encoded) }.getOrNull()
        }

    override suspend fun write(directory: CachedNodeDirectory) {
        check(
            preferences
                .edit()
                .putString(DIRECTORY_KEY, json.encodeToString(directory))
                .commit()
        ) {
            "Signed node directory could not be cached"
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "securechat_node_directory"
        const val DIRECTORY_KEY = "signed_directory"
    }
}
