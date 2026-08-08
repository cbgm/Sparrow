package com.cbgm.securechat.feature.transport.discovery

import com.cbgm.securechat.core.crypto.hash.DefaultCryptoHash
import com.cbgm.securechat.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.securechat.feature.transport.relay.codec.createRelayJson
import com.cbgm.securechat.feature.transport.relay.config.RelayTransportConfig
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultNodeEndpointResolverTest {
    private val json = createRelayJson()
    private val cryptoHash = DefaultCryptoHash()

    @Test
    fun validRemoteDirectoryIsCachedAndReusedUntilRefresh() =
        runTest {
            val directory = signedDirectory()
            val source = RecordingNodeDirectorySource(Result.success(json.encodeToString(directory)))
            val cache = RecordingNodeDirectoryCache()
            val resolver = resolver(source = source, cache = cache, now = { NOW })

            val first = resolver.resolve("relay-a").getOrThrow()
            val second = resolver.resolve("relay-a").getOrThrow()

            assertEquals(
                setOf("wss://a.example/relay", "wss://b.example/relay"),
                first.map(NodeEndpoint::websocketUrl).toSet()
            )
            assertEquals(first, second)
            assertEquals(1, source.fetchCount)
            assertEquals(directory.authorityNodeId, cache.value?.trustedAuthorityNodeId)
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
                        trustedAuthorityNodeId = directory.authorityNodeId
                    )
                )
            val source =
                RecordingNodeDirectorySource(
                    Result.failure(IllegalStateException("registry unavailable"))
                )
            val resolver = resolver(source = source, cache = cache, now = { now })

            now = NOW + 2_000L
            val result = resolver.resolve("relay-a")

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
                        trustedAuthorityNodeId = directory.authorityNodeId
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

            assertTrue(resolver.resolve("relay-a").isFailure)
        }

    private fun resolver(
        source: NodeDirectorySource,
        cache: NodeDirectoryCache,
        now: () -> Long
    ): DefaultNodeEndpointResolver {
        val trustedDirectory = signedDirectory()
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
                RelayTransportConfig(
                    httpBaseUrl = "http://localhost:8095",
                    nodeRegistryBaseUrl = "https://registry.example",
                    trustedRegistryAuthorityNodeId = trustedDirectory.authorityNodeId,
                    directoryRefreshIntervalMilliseconds = 60_000L,
                    cachedDirectoryGraceMilliseconds = CACHE_GRACE_MILLISECONDS
                ),
            now = now
        )
    }

    private fun signedDirectory(directoryValidUntil: Long = NOW + 60_000L): SignedNodeDirectory {
        val authorityKey = encodedPublicKey(seed = 1)
        return SignedNodeDirectory(
            directory =
                NodeDirectory(
                    generatedAtEpochMilliseconds = NOW - 1_000L,
                    validUntilEpochMilliseconds = directoryValidUntil,
                    nodes =
                        listOf(
                            descriptor("node-a", "wss://a.example/relay", seed = 2),
                            descriptor("node-b", "wss://b.example/relay", seed = 3)
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
        seed: Int
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
            validUntilEpochMilliseconds = NOW + 10L * 60L * 1_000L,
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
        private val result: Result<String>
    ) : NodeDirectorySource {
        var fetchCount: Int = 0

        override suspend fun fetch(registryBaseUrl: String): Result<String> {
            fetchCount += 1
            return result
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
