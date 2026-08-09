package com.cbgm.securechat.feature.transport.controlplane

import com.cbgm.securechat.core.transport.ControlPlaneConfiguration
import com.cbgm.securechat.core.transport.ControlPlaneEndpoint
import com.cbgm.securechat.core.transport.ControlPlaneEndpointStatus
import com.cbgm.securechat.core.transport.ControlPlaneReachability
import com.cbgm.securechat.core.transport.ControlPlaneStatusStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
            assertEquals("https://secondary", configuration.activeEndpoint.value?.baseUrl)
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
