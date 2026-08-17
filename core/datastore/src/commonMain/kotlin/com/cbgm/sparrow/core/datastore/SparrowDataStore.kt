package com.cbgm.sparrow.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SparrowDataStore internal constructor(
    private val dataStore: DataStore<Preferences>
) {
    fun observeString(key: String): Flow<String?> =
        dataStore.data
            .map { preferences -> preferences[stringPreferencesKey(key)] }
            .distinctUntilChanged()

    fun observeLong(
        key: String,
        defaultValue: Long = 0L
    ): Flow<Long> =
        dataStore.data
            .map { preferences -> preferences[longPreferencesKey(key)] ?: defaultValue }
            .distinctUntilChanged()

    suspend fun getString(key: String): String? =
        dataStore.data.first()[stringPreferencesKey(key)]

    suspend fun getLong(
        key: String,
        defaultValue: Long = 0L
    ): Long =
        dataStore.data.first()[longPreferencesKey(key)] ?: defaultValue

    suspend fun getBoolean(
        key: String,
        defaultValue: Boolean = false
    ): Boolean =
        dataStore.data.first()[booleanPreferencesKey(key)] ?: defaultValue

    suspend fun containsString(key: String): Boolean =
        dataStore.data.first()[stringPreferencesKey(key)] != null

    suspend fun keys(prefix: String): Set<String> =
        dataStore.data
            .first()
            .asMap()
            .keys
            .map { key -> key.name }
            .filterTo(mutableSetOf()) { key -> key.startsWith(prefix) }

    suspend fun edit(block: SparrowDataStoreEditor.() -> Unit) {
        dataStore.edit { preferences ->
            SparrowDataStoreEditor(preferences).block()
        }
    }
}

class SparrowDataStoreEditor internal constructor(
    private val preferences: MutablePreferences
) {
    fun putString(
        key: String,
        value: String
    ) {
        preferences[stringPreferencesKey(key)] = value
    }

    fun putLong(
        key: String,
        value: Long
    ) {
        preferences[longPreferencesKey(key)] = value
    }

    fun putBoolean(
        key: String,
        value: Boolean
    ) {
        preferences[booleanPreferencesKey(key)] = value
    }

    fun removeString(key: String) {
        preferences.remove(stringPreferencesKey(key))
    }

    fun removeLong(key: String) {
        preferences.remove(longPreferencesKey(key))
    }

    fun removeBoolean(key: String) {
        preferences.remove(booleanPreferencesKey(key))
    }

    fun clear() {
        preferences.clear()
    }
}
