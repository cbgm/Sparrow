package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.RELAY_CONTEXT
import com.cbgm.sparrow.server.protocol.RelayExit
import com.cbgm.sparrow.server.protocol.RelayExitResponse
import com.cbgm.sparrow.server.protocol.RelayForward
import com.cbgm.sparrow.server.protocol.RelayLayer
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.ReplySealing
import com.cbgm.sparrow.server.security.X25519KeyPair
import com.cbgm.sparrow.server.security.X25519Sealing
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnionRelayTest {
    private val firstKeys = X25519KeyPair.generate()
    private val secondKeys = X25519KeyPair.generate()
    private val seenByControlPlane = mutableListOf<String>()
    private lateinit var second: OnionRelay

    private val network =
        HttpClient(
            MockEngine { request ->
                when (request.url.toString()) {
                    "https://relay-b.test/v1/relay" -> {
                        when (val reply = runBlocking { second.relay(request.body.toByteArray()) }) {
                            is RelayReply.Sealed -> respond(reply.bytes, HttpStatusCode.OK)
                            RelayReply.Refused -> respond("", HttpStatusCode.BadRequest)
                            RelayReply.NextRelayFailed -> respond("", HttpStatusCode.BadGateway)
                            RelayReply.ControlPlaneFailed -> respond("", HttpStatusCode.GatewayTimeout)
                        }
                    }
                    "https://control.test/v1/nodes" -> {
                        seenByControlPlane += request.headers.names().joinToString()
                        respond("""{"directory":"signed"}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                    }
                    "https://down.test/v1/nodes" -> error("Connection refused")
                    else -> respond("", HttpStatusCode.NotFound)
                }
            }
        ) { expectSuccess = false }
    private val policy = RelayDestinationPolicy(allowPrivateAddresses = false, resolve = { listOf(PUBLIC_ADDRESS) })
    private val first = OnionRelay(firstKeys, network, policy)

    init {
        second = OnionRelay(secondKeys, network, policy)
    }

    @Test
    fun twoRelaysCarryARequestNeitherCanRead() =
        runTest {
            val innerReplyKey = ByteArray(ReplySealing.KEY_BYTES) { 2 }
            val outerReplyKey = ByteArray(ReplySealing.KEY_BYTES) { 1 }
            val inner = seal(RelayLayer(innerReplyKey, exit = RelayExit("GET", "https://control.test/v1/nodes")), secondKeys)
            val outer = seal(RelayLayer(outerReplyKey, forward = RelayForward("https://relay-b.test/v1/relay", inner)), firstKeys)

            val reply = assertIs<RelayReply.Sealed>(first.relay(outer)).bytes
            val exitReply = assertNotNull(ReplySealing.open(assertNotNull(ReplySealing.open(reply, outerReplyKey)), innerReplyKey))
            val response = serverJson.decodeFromString<RelayExitResponse>(exitReply.decodeToString())

            assertEquals(200, response.status)
            assertEquals("""{"directory":"signed"}""", response.body.decodeToString())
            // The first relay could open only the forward layer, never the exit.
            assertNull(firstKeys.open(inner, RELAY_CONTEXT))
            assertEquals(1, seenByControlPlane.size)
        }

    @Test
    fun aLayerForAnotherRelayIsRefused() =
        runTest {
            val layer = seal(RelayLayer(ByteArray(ReplySealing.KEY_BYTES), exit = RelayExit("GET", "https://control.test/v1/nodes")), secondKeys)

            assertEquals(RelayReply.Refused, first.relay(layer))
        }

    @Test
    fun anUnreachableControlPlaneIsReportedThroughBothRelays() =
        runTest {
            val inner = seal(RelayLayer(ByteArray(ReplySealing.KEY_BYTES) { 2 }, exit = RelayExit("GET", "https://down.test/v1/nodes")), secondKeys)
            val outer =
                seal(
                    RelayLayer(ByteArray(ReplySealing.KEY_BYTES) { 1 }, forward = RelayForward("https://relay-b.test/v1/relay", inner)),
                    firstKeys
                )

            // Not the second relay's fault: the app must not leave it out.
            assertEquals(RelayReply.ControlPlaneFailed, first.relay(outer))
        }

    @Test
    fun aFailingNextRelayIsReportedAsSuch() =
        runTest {
            val inner = seal(RelayLayer(ByteArray(ReplySealing.KEY_BYTES) { 2 }, exit = RelayExit("GET", "https://control.test/v1/nodes")), secondKeys)
            val outer =
                seal(
                    RelayLayer(ByteArray(ReplySealing.KEY_BYTES) { 1 }, forward = RelayForward("https://relay-c.test/v1/relay", inner)),
                    firstKeys
                )

            assertEquals(RelayReply.NextRelayFailed, first.relay(outer))
        }

    @Test
    fun relaysReachOnlyControlPlanePathsAndOtherRelays() {
        assertNotNull(policy.controlPlane(RelayExit("GET", "https://control.test/v1/nodes")))
        assertNotNull(policy.controlPlane(RelayExit("POST", "https://control.test/v1/alias-claims")))
        assertNull(policy.controlPlane(RelayExit("POST", "https://control.test/push/devices")))
        assertNull(policy.controlPlane(RelayExit("POST", "https://control.test/v1/nodes")))
        assertNull(policy.controlPlane(RelayExit("GET", "https://control.test/v1/routes/scphone1_x")))
        assertNull(policy.controlPlane(RelayExit("GET", "https://user@control.test/v1/nodes")))
        assertNull(policy.controlPlane(RelayExit("GET", "file:///etc/passwd")))
        assertNotNull(policy.sessionRequest("POST", "https://box.test/v1/mailboxes/wait"))
        assertNotNull(policy.sessionRequest("POST", "https://box.test/v1/mailboxes/box-1/envelopes"))
        assertNull(policy.sessionRequest("GET", "https://box.test/v1/mailboxes/wait"))
        assertNull(policy.sessionRequest("POST", "https://box.test/v1/mailboxes/wait?all=1"))
        assertNotNull(policy.sessionRequest("PUT", "https://node.test/v1/blobs/blob-0123456789abcdef/chunks/3"))
        assertNotNull(policy.sessionRequest("GET", "https://node.test/v1/blobs/blob-0123456789abcdef/chunks"))
        assertNull(policy.sessionRequest("PUT", "https://node.test/v1/blobs/blob-0123456789abcdef"))
        assertNull(policy.sessionRequest("GET", "https://node.test/v1/blobs/blob-0123456789abcdef/chunks/../../internal"))
        assertNotNull(policy.nextRelay("https://relay-b.test/v1/relay"))
        assertNull(policy.nextRelay("https://relay-b.test/v1/relay?x=1"))
        assertNull(policy.nextRelay("https://relay-b.test/internal/v1/routes"))
    }

    @Test
    fun privateAddressesAreRefusedOutsideLanInstallations() {
        val lan = InetAddress.getByName("192.168.1.10")
        val exit = RelayExit("GET", "http://control.lan/v1/nodes")

        assertNull(RelayDestinationPolicy(allowPrivateAddresses = false, resolve = { listOf(lan) }).controlPlane(exit))
        assertNull(RelayDestinationPolicy(allowPrivateAddresses = false, resolve = { listOf(InetAddress.getLoopbackAddress()) }).controlPlane(exit))
        assertTrue(RelayDestinationPolicy(allowPrivateAddresses = true, resolve = { listOf(lan) }).controlPlane(exit) != null)
        assertFalse(PUBLIC_ADDRESS.isLoopbackAddress)
    }

    private fun seal(
        layer: RelayLayer,
        keys: X25519KeyPair
    ): ByteArray = X25519Sealing.seal(serverJson.encodeToString(layer).encodeToByteArray(), keys.publicKey, RELAY_CONTEXT)

    private companion object {
        val PUBLIC_ADDRESS: InetAddress = InetAddress.getByAddress(byteArrayOf(93, -72, -40, 34))
    }
}
