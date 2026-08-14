package com.cbgm.securechat.feature.transport.discovery

import com.cbgm.securechat.core.crypto.hash.DefaultCryptoHash
import com.cbgm.securechat.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.securechat.core.transport.ControlPlaneConfiguration
import com.cbgm.securechat.core.transport.ControlPlaneEndpoint
import com.cbgm.securechat.core.transport.ControlPlaneEndpointStatus
import com.cbgm.securechat.core.transport.ControlPlaneReachability
import com.cbgm.securechat.core.transport.ControlPlaneStatusStore
import com.cbgm.securechat.feature.transport.config.TransportConfig
import com.cbgm.securechat.feature.transport.gateway.codec.createGatewayJson
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
    fun firstResolutionFetchesFreshLoadInsteadOfUsingProcessCache() =
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

            assertEquals("wss://b.example/v1/gateway", endpoints.first().websocketUrl)
            assertEquals(1, source.fetchCount)
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
                    now = { NOW }
                )

            val result = resolver.resolve("routing-a")

            assertTrue(result.isSuccess)
            assertEquals("https://cp-b.example", configuration.activeEndpoint.value?.baseUrl)
            assertEquals(2, source.fetchCount)
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
            now = now
        )
    }

    private fun signedDirectory(
        directoryValidUntil: Long = NOW + 60_000L,
        descriptorValidUntil: Long = NOW + 10L * 60L * 1_000L,
        nodes: List<SecureChatNodeDescriptor>? = null
    ): SignedNodeDirectory {
        val authorityKey = encodedPublicKey(seed = 1)
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
    ): SecureChatNodeDescriptor {
        val key = encodedPublicKey(seed)
        return SecureChatNodeDescriptor(
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
