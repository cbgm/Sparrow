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

    @Test
    fun theFirstHopIsTheNodeTheAppIsConnectedThrough() =
        runTest {
            val firstHops = mutableListOf<String>()
            val client =
                onionClient(
                    nodes = listOf(node("a", 1), node("b", 2), node("c", 3)),
                    preferred = "a",
                    connected = { "c" },
                    network = { url, body ->
                        firstHops += url
                        relayNetwork(url, body)
                    }
                )

            repeat(5) { client.send(RelayExit("GET", "https://control.test/v1/nodes")) }

            assertEquals(List(5) { "https://c.test/v1/relay" }, firstHops)
        }

    @Test
    fun aFailedSecondRelayIsLeftOutAndTheRequestStillGoesThrough() =
        runTest {
            val firstHops = mutableListOf<String>()
            val client =
                onionClient(
                    nodes = listOf(node("own", 1), node("down", 2)),
                    preferred = "own",
                    network = { url, body ->
                        firstHops += url
                        relayNetwork(url, body, failing = setOf("https://down.test/v1/relay"))
                    }
                )

            assertEquals(200, client.send(RelayExit("GET", "https://control.test/v1/nodes"))?.status)
            // The retry and the next request use the remaining relay alone.
            assertEquals(200, client.send(RelayExit("GET", "https://control.test/v1/nodes"))?.status)
            assertEquals(3, firstHops.size)
        }

    @Test
    fun aFailedSecondRelayIsReplacedByAThirdNode() =
        runTest {
            val routes = mutableListOf<List<String>>()
            val client =
                onionClient(
                    nodes = listOf(node("own", 1), node("down", 2), node("third", 3)),
                    preferred = null,
                    connected = { "own" },
                    network = { url, body ->
                        relayNetwork(url, body, failing = setOf("https://down.test/v1/relay"), path = routes)
                    }
                )

            repeat(4) { assertEquals(200, client.send(RelayExit("GET", "https://control.test/v1/nodes"))?.status) }

            // Every request that reached the Control Plane went through the own node and the third one.
            assertTrue(routes.isNotEmpty())
            assertTrue(routes.all { route -> route == listOf("https://own.test/v1/relay", "https://third.test/v1/relay") })
        }

    @Test
    fun anUnreachableFirstRelayIsLeftOut() =
        runTest {
            val firstHops = mutableListOf<String>()
            val client =
                onionClient(
                    nodes = listOf(node("own", 1), node("other", 2)),
                    preferred = null,
                    connected = { "own" },
                    network = { url, body ->
                        firstHops += url
                        check(url != "https://own.test/v1/relay") { "Connection refused" }
                        relayNetwork(url, body)
                    }
                )

            assertEquals(200, client.send(RelayExit("GET", "https://control.test/v1/nodes"))?.status)
            assertEquals(listOf("https://own.test/v1/relay", "https://other.test/v1/relay"), firstHops)
        }

    @Test
    fun aFailedRelayIsTriedAgainAfterItsCooldown() =
        runTest {
            var clock = 1L
            var downIsDown = true
            val routes = mutableListOf<Int>()
            val client =
                onionClient(
                    nodes = listOf(node("own", 1), node("down", 2)),
                    preferred = "own",
                    now = { clock },
                    network = { url, body ->
                        val failing = if (downIsDown) setOf("https://down.test/v1/relay") else emptySet()
                        relayNetwork(url, body, failing = failing, hops = routes)
                    }
                )

            client.send(RelayExit("GET", "https://control.test/v1/nodes"))
            downIsDown = false
            clock = 1L + COOLDOWN
            routes.clear()
            client.send(RelayExit("GET", "https://control.test/v1/nodes"))

            assertEquals(listOf(2), routes)
        }

    @Test
    fun anUnreachableControlPlaneLeavesNoRelayOut() =
        runTest {
            var calls = 0
            val client =
                onionClient(
                    nodes = listOf(node("own", 1), node("other", 2)),
                    preferred = "own",
                    status = { HttpStatusCode.GatewayTimeout },
                    network = { _, _ ->
                        calls++
                        byteArrayOf()
                    }
                )

            assertFailsWith<IllegalStateException> { client.send(RelayExit("GET", "https://control.test/v1/nodes")) }
            assertFailsWith<IllegalStateException> { client.send(RelayExit("GET", "https://control.test/v1/nodes")) }
            // No retry: both requests went once through the same two relays.
            assertEquals(2, calls)
        }

    /**
     * Plays both relays: peels the layer for [url] and either passes it to the next relay or answers as the exit.
     * A relay in [failing] does not answer, so the relay before it reports 502. [hops] collects each route's length.
     */
    private fun relayNetwork(
        url: String,
        body: ByteArray,
        failing: Set<String> = emptySet(),
        hops: MutableList<Int>? = null,
        depth: Int = 1,
        path: MutableList<List<String>>? = null,
        visited: List<String> = emptyList()
    ): ByteArray {
        val layer = peel(body)
        val forward = layer.forward
        val reply =
            if (forward == null) {
                hops?.add(depth)
                path?.add(visited + url)
                json.encodeToString(RelayExitResponse(200, "application/json", "{}".encodeToByteArray())).encodeToByteArray()
            } else {
                if (forward.relayUrl in failing) throw RelayDown()
                relayNetwork(forward.relayUrl, forward.layer, failing, hops, depth + 1, path, visited + url)
            }
        return cipher.sealReply(reply, layer.replyKey)
    }

    private fun peel(body: ByteArray): RelayLayer = json.decodeFromString(body.copyOfRange(1, body.size).decodeToString())

    private class RelayDown : RuntimeException()

    private fun onionClient(
        nodes: List<SparrowNodeDescriptor>,
        preferred: String?,
        connected: () -> String? = { null },
        now: () -> Long = { 1L },
        status: (String) -> HttpStatusCode = { HttpStatusCode.OK },
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
        // Like the app's shared client: failed answers would be thrown unless the request asks otherwise.
        val httpClient =
            HttpClient(
                MockEngine { request ->
                    val url = request.url.toString()
                    runCatching { network(url, request.body.toByteArray()) }.fold(
                        onSuccess = { reply -> respond(reply, status(url)) },
                        onFailure = { failure -> if (failure is RelayDown) respond(byteArrayOf(), HttpStatusCode.BadGateway) else throw failure }
                    )
                }
            ) { expectSuccess = true }
        val selector =
            RelaySelector(
                cache = cache,
                json = json,
                preferredNodeStore = store,
                connectedNodeId = connected,
                now = now,
                random = Random(1),
                failureCooldownMilliseconds = COOLDOWN
            )
        return OnionRelayClient(selector, cipher, httpClient, json)
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

    private companion object {
        const val COOLDOWN = 5L
    }
}
