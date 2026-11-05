package com.cbgm.sparrow.feature.transport.relay

import com.cbgm.sparrow.core.crypto.sealing.SealedMessageCipher
import com.cbgm.sparrow.feature.transport.discovery.CachedNodeDirectory
import com.cbgm.sparrow.feature.transport.discovery.NodeCapability
import com.cbgm.sparrow.feature.transport.discovery.NodeDirectory
import com.cbgm.sparrow.feature.transport.discovery.NodeDirectoryCache
import com.cbgm.sparrow.feature.transport.discovery.PreferredNodeStore
import com.cbgm.sparrow.feature.transport.discovery.SignedNodeDirectory
import com.cbgm.sparrow.feature.transport.discovery.SparrowNodeDescriptor
import com.cbgm.sparrow.feature.transport.gateway.codec.createGatewayJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnionRelayClientTest {
    private val json = createGatewayJson()
    private val cipher = ReadableTestCipher()

    @Test
    fun theOwnNodeRelaysToAnotherNodeWhichMakesTheRequest() =
        runTest {
            val seen = mutableListOf<String>()
            val client =
                onionClient(
                    nodes = listOf(node("own", 1), node("other", 2)),
                    preferred = "own",
                    network = { url, body ->
                        seen += url
                        val outer = cipher.openLayer(body, key = 1)
                        val forward = outer.forward!!
                        assertEquals("https://other.test/v1/relay", forward.relayUrl)
                        val inner = cipher.openLayer(forward.layer, key = 2)
                        assertEquals("https://control.test/v1/nodes", inner.exit!!.url)
                        val response = json.encodeToString(RelayExitResponse(200, "application/json", "{}".encodeToByteArray()))
                        cipher.sealReply(cipher.sealReply(response.encodeToByteArray(), inner.replyKey), outer.replyKey)
                    }
                )

            val response = client.send(RelayExit("GET", "https://control.test/v1/nodes"))

            assertEquals(listOf("https://own.test/v1/relay"), seen)
            assertEquals(200, response?.status)
            assertEquals("{}", response?.body?.decodeToString())
        }

    @Test
    fun aSingleRelayingNodeIsTheOnlyHop() =
        runTest {
            val client =
                onionClient(
                    nodes = listOf(node("own", 1), node("old", null)),
                    preferred = "own",
                    network = { _, body ->
                        val layer = cipher.openLayer(body, key = 1)
                        assertEquals("https://control.test/health/registry", layer.exit!!.url)
                        cipher.sealReply(json.encodeToString(RelayExitResponse(204)).encodeToByteArray(), layer.replyKey)
                    }
                )

            assertEquals(204, client.send(RelayExit("GET", "https://control.test/health/registry"))?.status)
        }

    @Test
    fun withoutRelayingNodesThereIsNoRoute() =
        runTest {
            val client = onionClient(nodes = listOf(node("old", null)), preferred = null, network = { _, _ -> error("No request") })

            assertNull(client.send(RelayExit("GET", "https://control.test/v1/nodes")))
        }

    @Test
    fun aRequestThatMustUseTheRelaysFailsWithoutThem() =
        runTest {
            val onion = onionClient(nodes = emptyList(), preferred = null, network = { _, _ -> error("No request") })
            val direct = HttpClient(MockEngine { respond("direct", HttpStatusCode.OK) })
            val routeOnly = createControlPlaneHttpClient(OnionRelayEngine(onion, direct) { RelayRequirement.ROUTE_ONLY }, json)
            val preferred = createControlPlaneHttpClient(OnionRelayEngine(onion, direct) { RelayRequirement.ROUTE_PREFERRED }, json)

            assertFailsWith<IllegalStateException> { routeOnly.post("https://control.test/v1/alias-claims") }
            assertEquals(HttpStatusCode.OK, preferred.get("https://control.test/v1/nodes").status)
        }

    @Test
    fun relayUrlsComeFromTheGatewayEndpoint() {
        assertEquals("https://node.test/v1/relay", relayUrl("wss://node.test/v1/gateway"))
        assertEquals("http://10.0.0.2:8494/v1/relay", relayUrl("ws://10.0.0.2:8494/v1/gateway"))
        assertNull(relayUrl("https://node.test/v1/gateway"))
        assertTrue(Url("https://node.test/v1/relay").encodedPath.endsWith("/v1/relay"))
    }

    private fun onionClient(
        nodes: List<SparrowNodeDescriptor>,
        preferred: String?,
        network: suspend (String, ByteArray) -> ByteArray
    ): OnionRelayClient {
        val directory =
            SignedNodeDirectory(
                directory = NodeDirectory(generatedAtEpochMilliseconds = 1L, validUntilEpochMilliseconds = 2L, nodes = nodes),
                authorityNodeId = "root",
                authorityPublicKey = byteArrayOf(1),
                signature = byteArrayOf(1)
            )
        val cache =
            object : NodeDirectoryCache {
                override suspend fun read() = CachedNodeDirectory(json.encodeToString(directory), "root")

                override suspend fun write(directory: CachedNodeDirectory) = Unit
            }
        val store =
            object : PreferredNodeStore {
                override fun observe(): Flow<String?> = flowOf(preferred)

                override suspend fun get(): String? = preferred

                override suspend fun set(nodeId: String?) = Unit
            }
        val httpClient =
            HttpClient(MockEngine { request -> respond(network(request.url.toString(), request.body.toByteArray()), HttpStatusCode.OK) })
        return OnionRelayClient(RelaySelector(cache, json, store, now = { 1L }, random = Random(1)), cipher, httpClient, json)
    }

    private fun node(
        name: String,
        relayKey: Int?
    ) = SparrowNodeDescriptor(
        nodeId = name,
        clientEndpoint = "wss://$name.test/v1/gateway",
        federationEndpoint = "https://$name.test",
        mailboxEndpoint = "https://$name.test",
        identityPublicKey = byteArrayOf(1),
        protocolVersions = setOf(1),
        capabilities = NodeCapability.entries.toSet(),
        validUntilEpochMilliseconds = 10L,
        relayKey = relayKey?.let { key -> ByteArray(32) { key.toByte() } },
        signature = byteArrayOf(1)
    )

    /** Stands in for libsodium (device tests cover the real cipher): readable, but checks the key it seals for. */
    private inner class ReadableTestCipher : SealedMessageCipher {
        private var nextReplyKey = 0

        override suspend fun seal(
            message: ByteArray,
            recipientPublicKey: ByteArray,
            context: String
        ): Result<ByteArray> = Result.success(byteArrayOf(recipientPublicKey.first()) + message)

        override suspend fun openReply(
            sealed: ByteArray,
            replyKey: ByteArray
        ): ByteArray? = sealed.takeIf { it.firstOrNull() == replyKey.first() }?.copyOfRange(1, sealed.size)

        override suspend fun newReplyKey(): ByteArray = ByteArray(32) { (100 + nextReplyKey).toByte() }.also { nextReplyKey++ }

        fun openLayer(
            sealed: ByteArray,
            key: Int
        ): RelayLayer {
            assertEquals(key.toByte(), sealed.first())
            return json.decodeFromString(sealed.copyOfRange(1, sealed.size).decodeToString())
        }

        fun sealReply(
            reply: ByteArray,
            replyKey: ByteArray
        ): ByteArray = byteArrayOf(replyKey.first()) + reply
    }
}
