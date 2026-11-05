package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.feature.transport.ControlPlaneDirectorySynchronizer
import com.cbgm.sparrow.feature.transport.ControlPlaneEndpoint
import com.cbgm.sparrow.feature.transport.controlplane.ControlPlaneUnavailableException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** A process started for a push while the app was closed has none of the app's startup. */
class PrepareControlPlaneUseCaseTest {
    @Test
    fun directoryControlPlanesAreRestoredFromTheVerifiedCache() =
        runTest {
            val configuration = FakeConfiguration()
            val directory = FakeDirectory(configuration, cached = listOf(DIRECTORY_PLANE))

            PrepareControlPlaneUseCase(configuration, directory)().getOrThrow()

            assertTrue(configuration.initialized)
            assertEquals(listOf(DIRECTORY_PLANE), configuration.orderedEndpoints().map { it.baseUrl })
            assertEquals(0, directory.refreshes)
        }

    @Test
    fun directoryIsFetchedOnlyWhenTheCacheGivesNoControlPlane() =
        runTest {
            val configuration = FakeConfiguration()
            val directory = FakeDirectory(configuration, cached = emptyList(), fetched = listOf(DIRECTORY_PLANE))

            PrepareControlPlaneUseCase(configuration, directory)().getOrThrow()

            assertEquals(1, directory.refreshes)
            assertEquals(listOf(DIRECTORY_PLANE), configuration.orderedEndpoints().map { it.baseUrl })
        }

    @Test
    fun savedManualControlPlaneNeedsNoDirectory() =
        runTest {
            val configuration = FakeConfiguration(saved = listOf("https://manual"))
            val directory = FakeDirectory(configuration, cached = emptyList())

            PrepareControlPlaneUseCase(configuration, directory)().getOrThrow()

            assertEquals(0, directory.refreshes)
        }

    @Test
    fun noControlPlaneAtAllIsReportedAsUnavailable() =
        runTest {
            val configuration = FakeConfiguration()
            val directory = FakeDirectory(configuration, cached = emptyList(), fetched = emptyList())

            val error = PrepareControlPlaneUseCase(configuration, directory)().exceptionOrNull()

            assertIs<ControlPlaneUnavailableException>(error)
        }

    private class FakeDirectory(
        private val configuration: FakeConfiguration,
        private val cached: List<String>,
        private val fetched: List<String> = emptyList()
    ) : ControlPlaneDirectorySynchronizer {
        var refreshes = 0

        override suspend fun restoreCached(): Result<Int> {
            configuration.directory += cached
            return Result.success(cached.size)
        }

        override suspend fun refresh(): Result<Int> {
            refreshes += 1
            configuration.directory += fetched
            return Result.success(fetched.size)
        }

        override suspend fun synchronizeFrom(url: String): Result<Int> = error("Not used")
    }

    /** Saved manual control planes appear only after initialize(), like the persisted configuration. */
    private class FakeConfiguration(
        private val saved: List<String> = emptyList()
    ) : ControlPlaneConfiguration {
        var initialized = false
        var manual = emptyList<String>()
        var directory = emptyList<String>()

        override val endpoints = MutableStateFlow(emptyList<ControlPlaneEndpoint>())
        override val activeEndpoint = MutableStateFlow<ControlPlaneEndpoint?>(null)
        override val manualBaseUrls = MutableStateFlow(emptySet<String>())
        override val directoryBaseUrls = MutableStateFlow(emptySet<String>())
        override val directoryUrl = MutableStateFlow<String?>(null)

        override suspend fun initialize() {
            initialized = true
            manual = saved
        }

        override fun orderedEndpoints(): List<ControlPlaneEndpoint> =
            (manual + directory).distinct().map(::ControlPlaneEndpoint)

        override fun markActive(endpoint: ControlPlaneEndpoint) = Unit

        override suspend fun replace(baseUrls: List<String>): Result<Unit> = error("Not used")

        override suspend fun addManual(baseUrl: String): Result<Unit> = error("Not used")

        override suspend fun removeManual(baseUrl: String): Result<Unit> = error("Not used")

        override suspend fun setDirectoryUrl(url: String?): Result<Unit> = error("Not used")

        override suspend fun replaceDirectory(baseUrls: List<String>): Result<Unit> = error("Not used")
    }

    private companion object {
        const val DIRECTORY_PLANE = "https://directory-plane"
    }
}
