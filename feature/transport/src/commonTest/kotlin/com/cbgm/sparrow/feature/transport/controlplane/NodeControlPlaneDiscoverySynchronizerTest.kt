package com.cbgm.sparrow.feature.transport.controlplane

import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.core.transport.ControlPlaneEndpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeControlPlaneDiscoverySynchronizerTest {
    @Test
    fun trustedCandidatesAreNormalizedAndMergedWithExistingDirectory() =
        runTest {
            val configuration =
                FakeControlPlaneConfiguration(
                    directoryUrls = setOf("https://existing.example")
                )
            val source =
                FakeNodeControlPlaneDirectorySource(
                    urls =
                        listOf(
                            "https://cp-a.example/",
                            "invalid value",
                            "https://cp-b.example",
                            "https://cp-a.example"
                        )
                )
            val verifier =
                FakeControlPlaneCandidateVerifier(
                    trusted = setOf("https://cp-a.example", "https://cp-b.example")
                )
            val synchronizer =
                NodeControlPlaneDiscoverySynchronizer(
                    source = source,
                    candidateVerifier = verifier,
                    configuration = configuration
                )

            val count = synchronizer.refreshFromNode("wss://node.example/v1/gateway").getOrThrow()

            assertEquals(2, count)
            assertEquals(
                setOf(
                    "https://existing.example",
                    "https://cp-a.example",
                    "https://cp-b.example"
                ),
                configuration.directoryBaseUrls.value
            )
            assertEquals(
                listOf("https://cp-a.example", "https://cp-b.example"),
                verifier.visited
            )
        }

    @Test
    fun untrustedCandidatesAreNotPersisted() =
        runTest {
            val configuration = FakeControlPlaneConfiguration()
            val synchronizer =
                NodeControlPlaneDiscoverySynchronizer(
                    source =
                        FakeNodeControlPlaneDirectorySource(
                            urls = listOf("https://untrusted.example")
                        ),
                    candidateVerifier = FakeControlPlaneCandidateVerifier(trusted = emptySet()),
                    configuration = configuration
                )

            val result = synchronizer.refreshFromNode("ws://node.example/v1/gateway")

            assertTrue(result.isFailure)
            assertTrue(configuration.directoryBaseUrls.value.isEmpty())
        }

    private class FakeNodeControlPlaneDirectorySource(
        private val urls: List<String>
    ) : NodeControlPlaneDirectorySource {
        override suspend fun fetch(websocketUrl: String): Result<List<String>> = Result.success(urls)
    }

    private class FakeControlPlaneCandidateVerifier(
        private val trusted: Set<String>
    ) : ControlPlaneCandidateVerifier {
        val visited = mutableListOf<String>()

        override suspend fun verify(baseUrl: String): Result<Unit> {
            visited += baseUrl
            return if (baseUrl in trusted) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("untrusted"))
            }
        }
    }

    private class FakeControlPlaneConfiguration(
        directoryUrls: Set<String> = emptySet()
    ) : ControlPlaneConfiguration {
        override val endpoints = MutableStateFlow<List<ControlPlaneEndpoint>>(emptyList())
        override val activeEndpoint = MutableStateFlow<ControlPlaneEndpoint?>(null)
        override val manualBaseUrls = MutableStateFlow(emptySet<String>())
        override val directoryBaseUrls = MutableStateFlow(directoryUrls)
        override val directoryUrl = MutableStateFlow<String?>(null)

        override fun orderedEndpoints(): List<ControlPlaneEndpoint> = endpoints.value

        override fun markActive(endpoint: ControlPlaneEndpoint) = Unit

        override suspend fun replace(baseUrls: List<String>): Result<Unit> = Result.success(Unit)

        override suspend fun addManual(baseUrl: String): Result<Unit> = Result.success(Unit)

        override suspend fun removeManual(baseUrl: String): Result<Unit> = Result.success(Unit)

        override suspend fun setDirectoryUrl(url: String?): Result<Unit> = Result.success(Unit)

        override suspend fun replaceDirectory(baseUrls: List<String>): Result<Unit> {
            directoryBaseUrls.value = baseUrls.toSet()
            return Result.success(Unit)
        }
    }
}
