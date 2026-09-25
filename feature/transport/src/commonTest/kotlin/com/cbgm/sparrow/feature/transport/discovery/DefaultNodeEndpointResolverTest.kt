package com.cbgm.sparrow.feature.transport.discovery

import com.cbgm.sparrow.core.crypto.hash.DefaultCryptoHash
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.core.transport.ControlPlaneEndpoint
import com.cbgm.sparrow.core.transport.ControlPlaneEndpointStatus
import com.cbgm.sparrow.core.transport.ControlPlaneReachability
import com.cbgm.sparrow.core.transport.ControlPlaneStatusStore
import com.cbgm.sparrow.feature.transport.config.TransportConfig
import com.cbgm.sparrow.feature.transport.gateway.codec.createGatewayJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultNodeEndpointResolverTest {
    private val json = createGatewayJson()
    private val cryptoHash = DefaultCryptoHash()

    @Test
    fun validRemoteDirectoryIsCachedAndReusedUntilRefresh() =
        runTest {
            val directory = signedDirectory()
            val source = RecordingNodeDirectorySource(Result.success(json.encodeToString(directory)))
            val cache = RecordingNodeDirectoryCache()
            val resolver = resolver(source = source, cache = cache, now = { NOW })

            val first = resolver.resolve("routing-a").getOrThrow()
            val second = resolver.resolve("routing-a").getOrThrow()

            assertEquals(
                setOf("wss://a.example/v1/gateway", "wss://b.example/v1/gateway"),
                first.map(NodeEndpoint::websocketUrl).toSet()
            )
            assertEquals(first, second)
            assertEquals(1, source.fetchCount)
            assertEquals(directory.authorityNodeId, cache.value?.trustedRootNodeId)
        }

    @Test
    fun firstResolutionUsesValidPersistentCacheWithoutNetworkFetch() =
        runTest {
            val cachedDirectory =
                signedDirectory(
                    nodes =
                        listOf(
                            descriptor(
                                name = "node-a",
                                endpoint = "wss://a.example/v1/gateway",
                                seed = 2,
                                activeConnections = 0
                            ),
                            descriptor(
                                name = "node-b",
                                endpoint = "wss://b.example/v1/gateway",
                                seed = 3,
                                activeConnections = 5
                            )
                        )
                )
            val remoteDirectory =
                signedDirectory(
                    nodes =
                        listOf(
                            descriptor(
                                name = "node-a",
                                endpoint = "wss://a.example/v1/gateway",
                                seed = 2,
                                activeConnections = 5
                            ),
                            descriptor(
                                name = "node-b",
                                endpoint = "wss://b.example/v1/gateway",
                                seed = 3,
                                activeConnections = 0
                            )
                        )
                )
            val source =
                RecordingNodeDirectorySource(
                    Result.success(json.encodeToString(remoteDirectory))
                )
            val cache =
                RecordingNodeDirectoryCache(
                    CachedNodeDirectory(
                        encodedDirectory = json.encodeToString(cachedDirectory),
                        trustedRootNodeId = cachedDirectory.authorityNodeId
                    )
                )
            val resolver = resolver(source = source, cache = cache, now = { NOW })

            val endpoints = resolver.resolve("routing-a").getOrThrow()

            assertEquals("wss://a.example/v1/gateway", endpoints.first().websocketUrl)
            assertEquals(0, source.fetchCount)
        }

    @Test
    fun forcedRefreshBypassesReusableDirectoryCache() =
        runTest {
            val initialDirectory =
                signedDirectory(
                    nodes =
                        listOf(
                            descriptor(
                                name = "node-a",
                                endpoint = "wss://a.example/v1/gateway",
                                seed = 2,
                                activeConnections = 1
                            )
                        )
                )
            val refreshedDirectory =
                signedDirectory(
                    nodes =
                        listOf(
                            descriptor(
                                name = "node-a",
                                endpoint = "wss://a.example/v1/gateway",
                                seed = 2,
                                activeConnections = 2
                            )
                        )
                )
            val source =
                RecordingNodeDirectorySource(
                    Result.success(json.encodeToString(initialDirectory))
                )
            val resolver =
                resolver(
                    source = source,
                    cache = RecordingNodeDirectoryCache(),
                    now = { NOW }
                )

            val initial = resolver.resolve("routing-a").getOrThrow()
            source.result = Result.success(json.encodeToString(refreshedDirectory))
            val refreshed =
                resolver
                    .resolve(
                        localRoutingId = "routing-a",
                        forceRefresh = true
                    ).getOrThrow()

            assertEquals(1, initial.single().activeConnections)
            assertEquals(2, refreshed.single().activeConnections)
            assertEquals(2, source.fetchCount)
        }

    @Test
    fun leastLoadedNodeIsPreferred() =
        runTest {
            val directory =
                signedDirectory(
                    nodes =
                        listOf(
                            descriptor(
                                name = "node-a",
                                endpoint = "wss://a.example/v1/gateway",
                                seed = 2,
                                activeConnections = 4
                            ),
                            descriptor(
                                name = "node-b",
                                endpoint = "wss://b.example/v1/gateway",
                                seed = 3,
                                activeConnections = 1
                            )
                        )
                )
            val resolver =
                resolver(
                    source =
                        RecordingNodeDirectorySource(
                            Result.success(json.encodeToString(directory))
                        ),
                    cache = RecordingNodeDirectoryCache(),
                    now = { NOW }
                )

            val endpoints = resolver.resolve("routing-a").getOrThrow()

            assertEquals("wss://b.example/v1/gateway", endpoints.first().websocketUrl)
            assertEquals(1, endpoints.first().activeConnections)
        }

    @Test
    fun registryFailureUsesRecentlyExpiredSignedCache() =
        runTest {
            var now = NOW
            val directory = signedDirectory(directoryValidUntil = NOW + 1_000L)
            val cache =
                RecordingNodeDirectoryCache(
                    CachedNodeDirectory(
                        encodedDirectory = json.encodeToString(directory),
                        trustedRootNodeId = directory.authorityNodeId
                    )
                )
            val source =
                RecordingNodeDirectorySource(
                    Result.failure(IllegalStateException("registry unavailable"))
                )
            val resolver = resolver(source = source, cache = cache, now = { now })

            now = NOW + 2_000L
            val result = resolver.resolve("routing-a")

            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrThrow().size)
        }

    @Test
    fun emptyRemoteDirectoryUsesRecentlyExpiredCachedDescriptors() =
        runTest {
            val cachedDirectory =
                signedDirectory(
                    directoryValidUntil = NOW + 1_000L,
                    descriptorValidUntil = NOW + 1_000L
                )
            val emptyRemoteDirectory =
                signedDirectory(
                    directoryValidUntil = NOW + 60_000L,
                    nodes = emptyList()
                )
            val cache =
                RecordingNodeDirectoryCache(
                    CachedNodeDirectory(
                        encodedDirectory = json.encodeToString(cachedDirectory),
                        trustedRootNodeId = cachedDirectory.authorityNodeId
                    )
                )
            val source =
                RecordingNodeDirectorySource(
                    Result.success(json.encodeToString(emptyRemoteDirectory))
                )
            val resolver =
                resolver(
                    source = source,
                    cache = cache,
                    now = { NOW + 2_000L }
                )

            val result = resolver.resolve("routing-a")

            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrThrow().size)
        }

    @Test
    fun cacheBeyondItsGracePeriodIsRejected() =
        runTest {
            val directory = signedDirectory(directoryValidUntil = NOW + 1_000L)
            val cache =
                RecordingNodeDirectoryCache(
                    CachedNodeDirectory(
                        encodedDirectory = json.encodeToString(directory),
                        trustedRootNodeId = directory.authorityNodeId
                    )
                )
            val resolver =
                resolver(
                    source =
                        RecordingNodeDirectorySource(
                            Result.failure(IllegalStateException("registry unavailable"))
                        ),
                    cache = cache,
                    now = { NOW + CACHE_GRACE_MILLISECONDS + 2_000L }
                )

            assertTrue(resolver.resolve("routing-a").isFailure)
        }

    @Test
    fun unavailablePreferredControlPlaneFallsBackToNext() =
        runTest {
            val directory = signedDirectory()
            val source =
                RecordingNodeDirectorySource(
                    result = Result.success(json.encodeToString(directory)),
                    failuresByBaseUrl = setOf("https://cp-a.example")
                )
            val configuration =
                FakeControlPlaneConfiguration(
                    listOf(
                        "https://cp-a.example",
                        "https://cp-b.example"
                    )
                )
            val trustedDirectory = signedDirectory()
            val resolver =
                DefaultNodeEndpointResolver(
                    source = source,
                    json = json,
                    cache = RecordingNodeDirectoryCache(),
                    verifier =
                        NodeDirectoryVerifier(
                            signatureCrypto = AcceptingSignatureCrypto,
                            cryptoHash = cryptoHash,
                            json = json
                        ),
                    config =
                        TransportConfig(
                            trustedRegistryRootNodeId = trustedDirectory.authorityNodeId
                        ),
                    controlPlaneConfiguration = configuration,
                    controlPlaneStatusStore = configuration,
                    endpointSelector = NodeEndpointSelector(config = TransportConfig()),
                    now = { NOW }
                )

            val result = resolver.resolve("routing-a")

            assertTrue(result.isSuccess)
            assertEquals("https://cp-b.example", configuration.activeEndpoint.value?.baseUrl)
            assertEquals(2, source.fetchCount)
        }

    @Test
    fun legacyCachePinnedToOldPlaneDoesNotBlockAnotherValidControlPlane() =
        runTest {
            val oldDirectory = signedDirectory(authoritySeed = 1)
            val newDirectory = signedDirectory(authoritySeed = 7)
            val cache =
                RecordingNodeDirectoryCache(
                    CachedNodeDirectory(
                        encodedDirectory = json.encodeToString(oldDirectory),
                        trustedRootNodeId = oldDirectory.authorityNodeId
                    )
                )
            val source =
                RecordingPerPlaneSource(
                    mapOf(
                        "http://192.168.178.21:8390" to
                            Result.failure(IllegalStateException("Connection timed out")),
                        "http://192.168.178.60:8390" to
                            Result.success(json.encodeToString(newDirectory))
                    )
                )
            val configuration =
                FakeControlPlaneConfiguration(
                    listOf("http://192.168.178.21:8390", "http://192.168.178.60:8390")
                )
            val resolver = perPlaneResolver(source, cache, configuration, now = { NOW + 11_000L })

            assertTrue(resolver.resolve("routing-a").isSuccess)
            assertEquals("http://192.168.178.60:8390", configuration.activeEndpoint.value?.baseUrl)
            assertEquals(
                listOf("http://192.168.178.21:8390", "http://192.168.178.60:8390"),
                source.visited
            )
            assertEquals("http://192.168.178.60:8390", cache.value?.sourceControlPlaneBaseUrl)
            assertEquals(
                newDirectory.authorityNodeId,
                cache.value?.trustedRootFor("http://192.168.178.60:8390")
            )
        }

    @Test
    fun pinnedRootForKnownPlaneIsNotSilentlyReplaced() =
        runTest {
            val trusted = signedDirectory(authoritySeed = 7)
            val replacement = signedDirectory(authoritySeed = 8)
            val url = "http://192.168.178.60:8390"
            val cache =
                RecordingNodeDirectoryCache(
                    CachedNodeDirectory(
                        encodedDirectory = json.encodeToString(trusted),
                        trustedRootNodeId = trusted.authorityNodeId,
                        sourceControlPlaneBaseUrl = url,
                        trustedRootsByControlPlane = mapOf(url to trusted.authorityNodeId)
                    )
                )
            val source = RecordingPerPlaneSource(mapOf(url to Result.success(json.encodeToString(replacement))))
            val resolver =
                perPlaneResolver(source, cache, FakeControlPlaneConfiguration(listOf(url)))

            resolver.resolve("routing-a", forceRefresh = true)
            assertEquals(trusted.authorityNodeId, cache.value?.trustedRootFor(url))
            assertEquals(listOf(url), source.visited)
        }

    @Test
    fun replacedManuallyConfiguredHttpsServerReconnectsWithoutReplaceServerAction() =
        runTest {
            val oldDirectory = signedDirectory(authoritySeed = 7)
            val newDirectory = signedDirectory(authoritySeed = 8)
            val url = "https://control.example"
            val cache =
                RecordingNodeDirectoryCache(
                    CachedNodeDirectory(
                        encodedDirectory = json.encodeToString(oldDirectory),
                        trustedRootNodeId = oldDirectory.authorityNodeId,
                        sourceControlPlaneBaseUrl = url,
                        trustedRootsByControlPlane = mapOf(url to oldDirectory.authorityNodeId)
                    )
                )
            val source = RecordingPerPlaneSource(
                mapOf(url to Result.success(json.encodeToString(newDirectory)))
            )
            val configuration = FakeControlPlaneConfiguration(listOf(url))
            val resolver = perPlaneResolver(source, cache, configuration)

            val result = resolver.resolve("routing-a", forceRefresh = true)
            assertTrue(result.isSuccess)
            assertEquals(newDirectory.authorityNodeId, cache.value?.trustedRootFor(url))
            assertEquals(url, cache.value?.sourceControlPlaneBaseUrl)
            assertEquals(url, configuration.activeEndpoint.value?.baseUrl)
        }

    @Test
    fun invalidReplacementDirectoryCannotReplaceStoredRegistryRoot() =
        runTest {
            val trusted = signedDirectory(authoritySeed = 7)

            val invalid = signedDirectory(authoritySeed = 8, nodes = emptyList())
            val url = "https://control.example"
            val cache =
                RecordingNodeDirectoryCache(
                    CachedNodeDirectory(
                        encodedDirectory = json.encodeToString(trusted),
                        trustedRootNodeId = trusted.authorityNodeId,
                        sourceControlPlaneBaseUrl = url,
                        trustedRootsByControlPlane = mapOf(url to trusted.authorityNodeId)
                    )
                )
            val resolver =
                perPlaneResolver(
                    RecordingPerPlaneSource(mapOf(url to Result.success(json.encodeToString(invalid)))),
                    cache,
                    FakeControlPlaneConfiguration(listOf(url))
                )
            resolver.resolve("routing-a", forceRefresh = true)
            assertEquals(trusted.authorityNodeId, cache.value?.trustedRootFor(url))
        }

    @Test
    fun discoveredHttpsPlaneCannotSilentlyReplacePreviouslyPinnedRoot() =
        runTest {
            val trusted = signedDirectory(authoritySeed = 7)
            val replacement = signedDirectory(authoritySeed = 8)
            val url = "https://directory-discovered.example"
            val cache =
                RecordingNodeDirectoryCache(
                    CachedNodeDirectory(
                        encodedDirectory = json.encodeToString(trusted),
                        trustedRootNodeId = trusted.authorityNodeId,
                        sourceControlPlaneBaseUrl = url,
                        trustedRootsByControlPlane = mapOf(url to trusted.authorityNodeId)
                    )
                )
            val configuration = FakeControlPlaneConfiguration(listOf(url))
            configuration.manualBaseUrls.value = emptySet()
            val resolver =
                perPlaneResolver(
                    RecordingPerPlaneSource(mapOf(url to Result.success(json.encodeToString(replacement)))),
                    cache,
                    configuration
                )
            resolver.resolve("routing-a", forceRefresh = true)
            assertEquals(trusted.authorityNodeId, cache.value?.trustedRootFor(url))
        }

    @Test
    fun previouslyVerifiedPlaneRootsSurviveFailoverInBothDirections() =
        runTest {
            val firstUrl = "https://cp-a.example"
            val secondUrl = "https://cp-b.example"
            val first = signedDirectory(authoritySeed = 1)
            val second = signedDirectory(authoritySeed = 7)
            val source =
                RecordingPerPlaneSource(
                    mapOf(
                        firstUrl to Result.success(json.encodeToString(first)),
                        secondUrl to Result.failure(IllegalStateException("offline"))
                    )
                )
            val cache = RecordingNodeDirectoryCache()
            val configuration = FakeControlPlaneConfiguration(listOf(firstUrl, secondUrl))
            val resolver = perPlaneResolver(source, cache, configuration)

            resolver.resolve("routing-a", forceRefresh = true).getOrThrow()
            source.results =
                mapOf(
                    firstUrl to Result.failure(IllegalStateException("offline")),
                    secondUrl to Result.success(json.encodeToString(second))
                )
            resolver.resolve("routing-a", forceRefresh = true).getOrThrow()
            assertEquals(first.authorityNodeId, cache.value?.trustedRootFor(firstUrl))
            assertEquals(second.authorityNodeId, cache.value?.trustedRootFor(secondUrl))

            source.results = mapOf(firstUrl to Result.success(json.encodeToString(first)))
            resolver.resolve("routing-a", forceRefresh = true).getOrThrow()
            assertEquals(firstUrl, configuration.activeEndpoint.value?.baseUrl)
        }

    private fun perPlaneResolver(
        source: NodeDirectorySource,
        cache: NodeDirectoryCache,
        configuration: FakeControlPlaneConfiguration,
        now: () -> Long = { NOW }
    ): DefaultNodeEndpointResolver =
        DefaultNodeEndpointResolver(
            source = source,
            json = json,
            cache = cache,
            verifier =
                NodeDirectoryVerifier(
                    signatureCrypto = AcceptingSignatureCrypto,
                    cryptoHash = cryptoHash,
                    json = json
                ),
            config = TransportConfig(trustedRegistryRootNodeId = null),
            controlPlaneConfiguration = configuration,
            controlPlaneStatusStore = configuration,
            endpointSelector = NodeEndpointSelector(config = TransportConfig()),
            now = now
        )

    private class RecordingPerPlaneSource(
        var results: Map<String, Result<String>>
    ) : NodeDirectorySource {
        val visited = mutableListOf<String>()

        override suspend fun fetch(registryBaseUrl: String): Result<String> {
            visited += registryBaseUrl
            return results[registryBaseUrl] ?: Result.failure(IllegalStateException("No fixture"))
        }
    }

    private fun resolver(
        source: NodeDirectorySource,
        cache: NodeDirectoryCache,
        now: () -> Long
    ): DefaultNodeEndpointResolver {
        val trustedDirectory = signedDirectory()
        val controlPlanes = FakeControlPlaneConfiguration()
        return DefaultNodeEndpointResolver(
            source = source,
            json = json,
            cache = cache,
            verifier =
                NodeDirectoryVerifier(
                    signatureCrypto = AcceptingSignatureCrypto,
                    cryptoHash = cryptoHash,
                    json = json
                ),
            config =
                TransportConfig(
                    trustedRegistryRootNodeId = trustedDirectory.authorityNodeId,
                    directoryRefreshIntervalMilliseconds = 60_000L,
                    cachedDirectoryGraceMilliseconds = CACHE_GRACE_MILLISECONDS
                ),
            controlPlaneConfiguration = controlPlanes,
            controlPlaneStatusStore = controlPlanes,
            endpointSelector = NodeEndpointSelector(config = TransportConfig()),
            now = now
        )
    }

    private fun signedDirectory(
        directoryValidUntil: Long = NOW + 60_000L,
        descriptorValidUntil: Long = NOW + 10L * 60L * 1_000L,
        nodes: List<SparrowNodeDescriptor>? = null,
        authoritySeed: Int = 1
    ): SignedNodeDirectory {
        val authorityKey = encodedPublicKey(seed = authoritySeed)
        return SignedNodeDirectory(
            directory =
                NodeDirectory(
                    generatedAtEpochMilliseconds = NOW - 1_000L,
                    validUntilEpochMilliseconds = directoryValidUntil,
                    nodes =
                        nodes
                            ?: listOf(
                                descriptor(
                                    "node-a",
                                    "wss://a.example/v1/gateway",
                                    seed = 2,
                                    validUntil = descriptorValidUntil
                                ),
                                descriptor(
                                    "node-b",
                                    "wss://b.example/v1/gateway",
                                    seed = 3,
                                    validUntil = descriptorValidUntil
                                )
                            )
                ),
            authorityNodeId = nodeId(authorityKey),
            authorityPublicKey = authorityKey,
            signature = byteArrayOf(1)
        )
    }

    private fun descriptor(
        name: String,
        endpoint: String,
        seed: Int,
        validUntil: Long = NOW + 10L * 60L * 1_000L,
        activeConnections: Int = 0
    ): SparrowNodeDescriptor {
        val key = encodedPublicKey(seed)
        return SparrowNodeDescriptor(
            nodeId = nodeId(key),
            clientEndpoint = endpoint,
            federationEndpoint = "https://$name.example/federation",
            mailboxEndpoint = "https://$name.example/mailbox",
            identityPublicKey = key,
            protocolVersions = setOf(1),
            capabilities = setOf(NodeCapability.GATEWAY),
            validUntilEpochMilliseconds = validUntil,
            activeConnections = activeConnections,
            signature = byteArrayOf(seed.toByte())
        )
    }

    private fun encodedPublicKey(seed: Int): ByteArray =
        X509_ED25519_PREFIX +
            ByteArray(32) { index ->
                (index + seed).toByte()
            }

    private fun nodeId(publicKey: ByteArray): String =
        cryptoHash
            .sha256(publicKey)
            .joinToString(separator = "") { byte ->
                (byte.toInt() and 0xff).toString(radix = 16).padStart(length = 2, padChar = '0')
            }

    private class RecordingNodeDirectorySource(
        var result: Result<String>,
        private val failuresByBaseUrl: Set<String> = emptySet()
    ) : NodeDirectorySource {
        var fetchCount: Int = 0

        override suspend fun fetch(registryBaseUrl: String): Result<String> {
            fetchCount += 1
            if (registryBaseUrl in failuresByBaseUrl) {
                return Result.failure(IllegalStateException("registry unavailable"))
            }
            return result
        }
    }

    private class FakeControlPlaneConfiguration(
        baseUrls: List<String> = listOf("https://registry.example")
    ) : ControlPlaneConfiguration,
        ControlPlaneStatusStore {
        private val configured = baseUrls.map(::ControlPlaneEndpoint)
        private val _endpoints = MutableStateFlow(configured)
        private val _activeEndpoint = MutableStateFlow(configured.firstOrNull())
        private val _statuses =
            MutableStateFlow(
                configured.map { endpoint ->
                    ControlPlaneEndpointStatus(
                        endpoint = endpoint,
                        isActive = endpoint == _activeEndpoint.value
                    )
                }
            )

        override val endpoints: StateFlow<List<ControlPlaneEndpoint>> = _endpoints
        override val activeEndpoint: StateFlow<ControlPlaneEndpoint?> = _activeEndpoint
        override val manualBaseUrls = MutableStateFlow(baseUrls.toSet())
        override val directoryBaseUrls = MutableStateFlow(emptySet<String>())
        override val directoryUrl = MutableStateFlow<String?>(null)
        override val statuses: StateFlow<List<ControlPlaneEndpointStatus>> = _statuses

        override fun orderedEndpoints(): List<ControlPlaneEndpoint> {
            val active = _activeEndpoint.value ?: return _endpoints.value
            return listOf(active) + _endpoints.value.filterNot { it == active }
        }

        override fun markActive(endpoint: ControlPlaneEndpoint) {
            _activeEndpoint.value = endpoint
            _statuses.value = _statuses.value.map { it.copy(isActive = it.endpoint == endpoint) }
        }

        override fun markAvailable(endpoint: ControlPlaneEndpoint) {
            updateStatus(endpoint, ControlPlaneReachability.AVAILABLE)
        }

        override fun markUnreachable(endpoint: ControlPlaneEndpoint) {
            updateStatus(endpoint, ControlPlaneReachability.UNREACHABLE)
        }

        override suspend fun replace(baseUrls: List<String>): Result<Unit> =
            runCatching {
                val updated = baseUrls.map(::ControlPlaneEndpoint)
                require(updated.isNotEmpty())
                _endpoints.value = updated
                _activeEndpoint.value = updated.first()
                _statuses.value =
                    updated.map { endpoint ->
                        ControlPlaneEndpointStatus(
                            endpoint = endpoint,
                            isActive = endpoint == updated.first()
                        )
                    }
            }

        override suspend fun addManual(baseUrl: String): Result<Unit> = Result.success(Unit)

        override suspend fun removeManual(baseUrl: String): Result<Unit> = Result.success(Unit)

        override suspend fun setDirectoryUrl(url: String?): Result<Unit> = Result.success(Unit)

        override suspend fun replaceDirectory(baseUrls: List<String>): Result<Unit> = Result.success(Unit)

        private fun updateStatus(
            endpoint: ControlPlaneEndpoint,
            reachability: ControlPlaneReachability
        ) {
            _statuses.value =
                _statuses.value.map { status ->
                    if (status.endpoint == endpoint) status.copy(reachability = reachability) else status
                }
        }
    }

    private class RecordingNodeDirectoryCache(
        var value: CachedNodeDirectory? = null
    ) : NodeDirectoryCache {
        override suspend fun read(): CachedNodeDirectory? = value

        override suspend fun write(directory: CachedNodeDirectory) {
            value = directory
        }
    }

    private object AcceptingSignatureCrypto : DetachedSignatureCrypto {
        override suspend fun sign(
            payload: ByteArray,
            signingPrivateKey: ByteArray
        ): Result<ByteArray> = Result.failure(UnsupportedOperationException())

        override suspend fun verify(
            payload: ByteArray,
            signingPublicKey: ByteArray,
            signature: ByteArray
        ): Result<Unit> = Result.success(Unit)
    }

    private companion object {
        const val NOW = 1_000_000L
        const val CACHE_GRACE_MILLISECONDS = 5L * 60L * 1_000L

        val X509_ED25519_PREFIX =
            byteArrayOf(
                0x30,
                0x2a,
                0x30,
                0x05,
                0x06,
                0x03,
                0x2b,
                0x65,
                0x70,
                0x03,
                0x21,
                0x00
            )
    }
}
