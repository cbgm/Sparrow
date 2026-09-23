package com.cbgm.sparrow.feature.transport.controlplane

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.cbgm.sparrow.data.datastore.createSparrowDataStore
import com.cbgm.sparrow.feature.transport.discovery.DataStoreNodeDirectoryCache
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Exercises the real persisted configuration without reinstalling or clearing app data. */
class ControlPlaneReaddAfterRemoveTest {
    @Test
    fun removedPublicPlaneCanBeReaddedImmediatelyWithoutManualRefresh(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = "control-plane-readd-${System.nanoTime()}.preferences_pb"
        val store = createSparrowDataStore(context.filesDir.resolve(file).absolutePath)
        val config = ControlPlaneConfigurationImpl(store, DataStoreNodeDirectoryCache(store, Json))
        val public = "https://control-87-171-165-152.sslip.io"
        val fallback = "https://other.example.test"

        config.initialize()
        config.replace(listOf(fallback, public)).getOrThrow()
        config.removeManual(public).getOrThrow()
        assertFalse(public in config.manualBaseUrls.value)
        // This used to depend on a manual Refresh between the two actions.
        config.addManual(public).getOrThrow()
        assertTrue(public in config.manualBaseUrls.value)
        assertEquals(1, config.endpoints.value.count { it.baseUrl == public })

        val reloaded = ControlPlaneConfigurationImpl(store, DataStoreNodeDirectoryCache(store, Json))
        reloaded.initialize()
        assertTrue(public in reloaded.manualBaseUrls.value)
    }

    @Test
    fun concurrentInitializationCannotOverwriteAnImmediateAddition(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = "control-plane-init-${System.nanoTime()}.preferences_pb"
        val store = createSparrowDataStore(context.filesDir.resolve(file).absolutePath)
        val config = ControlPlaneConfigurationImpl(store, DataStoreNodeDirectoryCache(store, Json))
        val public = "https://control-87-171-165-152.sslip.io"

        val initialize = async { config.initialize() }
        val add = async { config.addManual(public).getOrThrow() }
        initialize.await()
        add.await()
        assertTrue(public in config.manualBaseUrls.value)

        val reloaded = ControlPlaneConfigurationImpl(store, DataStoreNodeDirectoryCache(store, Json))
        reloaded.initialize()
        assertTrue(public in reloaded.manualBaseUrls.value)
    }
}
