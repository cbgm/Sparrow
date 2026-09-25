package com.cbgm.sparrow.feature.transport.controlplane

import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.core.transport.ControlPlaneEndpoint
import com.cbgm.sparrow.core.transport.ControlPlaneEndpointStatus
import com.cbgm.sparrow.core.transport.ControlPlaneReachability
import com.cbgm.sparrow.core.transport.ControlPlaneStatusStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ControlPlaneRequestRouterTest {
    @Test
    fun failedPrimaryFallsBackAndUpdatesHealth() =
        runTest {
            val configuration = FakeControlPlaneConfiguration()
            val router = ControlPlaneRequestRouter(configuration, configuration)

            val result =
                router.execute { endpoint ->
                    if (endpoint.baseUrl.endsWith("primary")) {
                        error("offline")
                    }
                    "ok"
                }

            assertEquals("ok", result.getOrThrow())
            assertEquals("https://secondary", configuration.activeEndpoint.value.baseUrl)
            assertEquals(
                ControlPlaneReachability.UNREACHABLE,
                configuration.statusFor("https://primary").reachability
            )
            assertEquals(
                ControlPlaneReachability.AVAILABLE,
                configuration.statusFor("https://secondary").reachability
            )
            assertTrue(configuration.statusFor("https://secondary").isActive)
        }

    @Test
    fun executeAllVisitsEveryEndpointAndSucceedsWhenOneIsAvailable() =
        runTest {
            val configuration = FakeControlPlaneConfiguration()
            val router = ControlPlaneRequestRouter(configuration, configuration)
            val visited = mutableListOf<String>()

            val result =
                router.executeAll { endpoint ->
                    visited += endpoint.baseUrl
                    if (endpoint.baseUrl.endsWith("primary")) {
                        error("offline")
                    }
                }

            assertTrue(result.isSuccess)
            assertEquals(
                listOf("https://primary", "https://secondary"),
                visited
            )
            assertEquals(
                ControlPlaneReachability.UNREACHABLE,
                configuration.statusFor("https://primary").reachability
            )
            assertEquals(
                ControlPlaneReachability.AVAILABLE,
                configuration.statusFor("https://secondary").reachability
            )
            assertEquals("https://secondary", configuration.activeEndpoint.value.baseUrl)
        }

    @Test
    fun executeAllFailsWhenNoEndpointCanCompleteTheRequest() =
        runTest {
            val configuration = FakeControlPlaneConfiguration()
            val router = ControlPlaneRequestRouter(configuration, configuration)
            val visited = mutableListOf<String>()

            val result = router.executeAll { endpoint ->
                visited += endpoint.baseUrl
                error("offline")
            }

            assertIs<ControlPlaneUnavailableException>(result.exceptionOrNull())
            assertEquals(listOf("https://primary", "https://secondary"), visited)
            assertEquals(
                ControlPlaneReachability.UNREACHABLE,
                configuration.statusFor("https://primary").reachability
            )
            assertEquals(
                ControlPlaneReachability.UNREACHABLE,
                configuration.statusFor("https://secondary").reachability
            )
        }

    @Test
    fun executeAllSkipsEndpointsAlreadyMarkedUnreachable() =
        runTest {
            val configuration = FakeControlPlaneConfiguration()
            configuration.markUnreachable(ControlPlaneEndpoint("https://primary"))
            val router = ControlPlaneRequestRouter(configuration, configuration)
            val visited = mutableListOf<String>()

            val result =
                router.executeAll { endpoint ->
                    visited += endpoint.baseUrl
                }

            assertTrue(result.isSuccess)
            assertEquals(listOf("https://secondary"), visited)
        }

    @Test
    fun executeAllReturnsOfflineResultWhenEveryEndpointWasAlreadyUnreachable() =
        runTest {
            val configuration = FakeControlPlaneConfiguration()
            configuration.endpoints.value.forEach(configuration::markUnreachable)
            val router = ControlPlaneRequestRouter(configuration, configuration)

            val result = router.executeAll { error("Must not attempt an unreachable endpoint") }

            assertIs<ControlPlaneUnavailableException>(result.exceptionOrNull())
        }

    @Test
    fun executeAllPreservesServerRejectionInsteadOfCallingItOffline() =
        runTest {
            val configuration = FakeControlPlaneConfiguration()
            val router = ControlPlaneRequestRouter(configuration, configuration)

            val result = router.executeAll {
                throw ControlPlaneRequestRejectedException("Push registration rejected")
            }

            assertIs<ControlPlaneRequestRejectedException>(result.exceptionOrNull())
            configuration.endpoints.value.forEach { endpoint ->
                assertEquals(
                    ControlPlaneReachability.AVAILABLE,
                    configuration.statusFor(endpoint.baseUrl).reachability
                )
            }
        }

    @Test
    fun firstAvailableStopsAfterFirstSuccessfulRegistration() =
        runTest {
            val configuration = FakeControlPlaneConfiguration()
            val router = ControlPlaneRequestRouter(configuration, configuration)
            val visited = mutableListOf<String>()

            val result = router.executeFirstAvailable { endpoint ->
                visited += endpoint.baseUrl
                "accepted"
            }

            assertEquals("accepted", result.getOrThrow())
            assertEquals(listOf("https://primary"), visited)
            assertEquals("https://primary", configuration.activeEndpoint.value.baseUrl)
        }

    @Test
    fun firstAvailableFallsBackEvenToPreviouslyUnreachableEndpoint() =
        runTest {
            val configuration = FakeControlPlaneConfiguration()
            configuration.markUnreachable(ControlPlaneEndpoint("https://secondary"))
            val router = ControlPlaneRequestRouter(configuration, configuration)
            val visited = mutableListOf<String>()

            val result = router.executeFirstAvailable { endpoint ->
                visited += endpoint.baseUrl
                if (endpoint.baseUrl == "https://primary") error("offline")
                "accepted"
            }

            assertEquals("accepted", result.getOrThrow())
            assertEquals(listOf("https://primary", "https://secondary"), visited)
            assertEquals("https://secondary", configuration.activeEndpoint.value.baseUrl)
            assertEquals(
                ControlPlaneReachability.AVAILABLE,
                configuration.statusFor("https://secondary").reachability
            )
        }

    @Test
    fun firstAvailableMarksServerRejectionAvailableAndAttemptsFallback() =
        runTest {
            val configuration = FakeControlPlaneConfiguration()
            val router = ControlPlaneRequestRouter(configuration, configuration)

            val result = router.executeFirstAvailable { endpoint ->
                if (endpoint.baseUrl == "https://primary") {
                    throw ControlPlaneRequestRejectedException("Registration rejected")
                }
                "accepted"
            }

            assertEquals("accepted", result.getOrThrow())
            assertEquals("https://secondary", configuration.activeEndpoint.value.baseUrl)
            assertEquals(
                ControlPlaneReachability.AVAILABLE,
                configuration.statusFor("https://primary").reachability
            )
        }

    @Test
    fun firstAvailableReturnsRetryableFailureWhenAllEndpointsAreOffline() =
        runTest {
            val configuration = FakeControlPlaneConfiguration()
            val router = ControlPlaneRequestRouter(configuration, configuration)
            val visited = mutableListOf<String>()
            val result = router.executeFirstAvailable { endpoint ->
                visited += endpoint.baseUrl
                error("offline")
            }

            assertIs<ControlPlaneUnavailableException>(result.exceptionOrNull())
            assertEquals(listOf("https://primary", "https://secondary"), visited)
        }

    @Test
    fun firstAvailableDoesNotTurnAnExplicitRejectionIntoOffline() =
        runTest {
            val configuration = FakeControlPlaneConfiguration()
            val router = ControlPlaneRequestRouter(configuration, configuration)

            val result = router.executeFirstAvailable { endpoint ->
                if (endpoint.baseUrl == "https://primary") {
                    throw ControlPlaneRequestRejectedException("Registration rejected")
                }
                error("offline")
            }

            assertIs<ControlPlaneRequestRejectedException>(result.exceptionOrNull())
            assertEquals(
                ControlPlaneReachability.AVAILABLE,
                configuration.statusFor("https://primary").reachability
            )
        }

    private class FakeControlPlaneConfiguration :
        ControlPlaneConfiguration,
        ControlPlaneStatusStore {
        override val endpoints =
            MutableStateFlow(
                listOf(
                    ControlPlaneEndpoint("https://primary"),
                    ControlPlaneEndpoint("https://secondary")
                )
            )
        override val activeEndpoint = MutableStateFlow(endpoints.value.first())
        override val manualBaseUrls = MutableStateFlow(endpoints.value.map { it.baseUrl }.toSet())
        override val directoryBaseUrls = MutableStateFlow(emptySet<String>())
        override val directoryUrl = MutableStateFlow<String?>(null)
        override val statuses =
            MutableStateFlow(
                endpoints.value.map { endpoint ->
                    ControlPlaneEndpointStatus(
                        endpoint = endpoint,
                        isActive = endpoint == activeEndpoint.value
                    )
                }
            )

        override fun orderedEndpoints(): List<ControlPlaneEndpoint> = endpoints.value

        override fun markActive(endpoint: ControlPlaneEndpoint) {
            activeEndpoint.value = endpoint
            statuses.value = statuses.value.map { it.copy(isActive = it.endpoint == endpoint) }
        }

        override suspend fun replace(baseUrls: List<String>): Result<Unit> = Result.success(Unit)

        override suspend fun addManual(baseUrl: String): Result<Unit> = Result.success(Unit)

        override suspend fun removeManual(baseUrl: String): Result<Unit> = Result.success(Unit)

        override suspend fun setDirectoryUrl(url: String?): Result<Unit> = Result.success(Unit)

        override suspend fun replaceDirectory(baseUrls: List<String>): Result<Unit> = Result.success(Unit)

        override fun markAvailable(endpoint: ControlPlaneEndpoint) {
            update(endpoint, ControlPlaneReachability.AVAILABLE)
        }

        override fun markUnreachable(endpoint: ControlPlaneEndpoint) {
            update(endpoint, ControlPlaneReachability.UNREACHABLE)
        }

        fun statusFor(url: String): ControlPlaneEndpointStatus =
            statuses.value.first { it.endpoint.baseUrl == url }

        private fun update(
            endpoint: ControlPlaneEndpoint,
            reachability: ControlPlaneReachability
        ) {
            statuses.value =
                statuses.value.map { status ->
                    if (status.endpoint == endpoint) status.copy(reachability = reachability) else status
                }
        }
    }
}
