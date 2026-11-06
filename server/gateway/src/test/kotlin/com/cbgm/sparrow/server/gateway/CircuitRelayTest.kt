package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.security.CircuitHandshake
import com.cbgm.sparrow.server.security.CircuitHopKeys
import com.cbgm.sparrow.server.security.CircuitLayer
import com.cbgm.sparrow.server.security.X25519KeyPair
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.request.receive
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.server.websocket.WebSockets as ServerWebSockets

class CircuitRelayTest {
    private val keysA = X25519KeyPair.generate()
    private val keysB = X25519KeyPair.generate()

    @Test
    fun aOneHopCircuitAnswersAPing() =
        relays {
            circuit {
                create(keysA)
                assertContentEquals("hello".encodeToByteArray(), ping("hello".encodeToByteArray()))
            }
        }

    @Test
    fun aCircuitExtendsToASecondRelayThatAnswersThroughTheFirst() =
        relays {
            circuit {
                create(keysA)
                assertEquals(RelayCommand.EXTENDED, extend("ws://relay-b/v1/circuit", keysB))
                assertContentEquals("through two".encodeToByteArray(), ping("through two".encodeToByteArray()))
                assertContentEquals("again".encodeToByteArray(), ping("again".encodeToByteArray()))
            }
        }

    @Test
    fun aRelayExtendsOnlyToAnotherRelaysCircuitPath() =
        relays {
            circuit {
                create(keysA)
                assertEquals(RelayCommand.EXTEND_FAILED, extend("ws://relay-b/v1/gateway", keysB))
            }
        }

    @Test
    fun theSessionNodeMakesALargeMailboxRequestThroughTwoHops() =
        relays {
            circuit {
                create(keysA)
                extend("ws://relay-b/v1/circuit", keysB)

                val answer =
                    request(
                        stream = 3,
                        request = CircuitHttpRequest("POST", "https://mailbox.test/v1/mailboxes/box-1/envelopes", "capability", "application/json", ByteArray(2_500) { 1 })
                    )

                assertEquals(RelayCommand.STREAM_END, answer.first)
                assertEquals(201, status(answer.second))
                assertEquals("box-1|Bearer capability|2500", body(answer.second).decodeToString())
            }
        }

    @Test
    fun aLargeAnswerComesBackInParts() =
        relays {
            circuit {
                create(keysA)
                val answer = request(stream = 1, request = CircuitHttpRequest("GET", "https://mailbox.test/v1/mailboxes/box-1/envelopes", "capability", null, ByteArray(0)))

                assertEquals(RelayCommand.STREAM_END, answer.first)
                assertContentEquals(ByteArray(LARGE_ANSWER_BYTES) { (it % 251).toByte() }, body(answer.second))
            }
        }

    @Test
    fun theSessionNodeRefusesAnythingButMailboxes() =
        relays {
            circuit {
                create(keysA)

                val answer = request(stream = 2, request = CircuitHttpRequest("GET", "https://mailbox.test/v1/routes/someone", null, null, ByteArray(0)))

                assertEquals(RelayCommand.STREAM_REFUSED, answer.first)
            }
        }

    @Test
    fun aCellThatDoesNotOpenEndsTheCircuit() =
        relays {
            circuit {
                create(keysA)
                session.send(Frame.Binary(true, CircuitCell(CIRCUIT_ID, CircuitCell.RELAY, ByteArray(1)).encode()))
                assertEquals(CircuitCell.DESTROY, receiveCell().command)
            }
        }

    private fun relays(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            install(ServerWebSockets)
            val policy = RelayDestinationPolicy(allowPrivateAddresses = false, resolve = { listOf(PUBLIC_ADDRESS) })
            val onward = createClient { install(ClientWebSockets) }
            val relayA = CircuitRelay(keysA, onward, policy, exitClient = onward)
            val relayB = CircuitRelay(keysB, onward, policy, exitClient = onward)
            externalServices {
                hosts("ws://relay-b") {
                    this.install(ServerWebSockets)
                    this.routing { installCircuitRoute(relayB) }
                }
                hosts("https://mailbox.test") {
                    this.routing {
                        post("/v1/mailboxes/{mailboxId}/envelopes") {
                            val token = call.request.headers[HttpHeaders.Authorization]
                            val body = call.receive<ByteArray>()
                            call.respondBytes(
                                "${call.parameters["mailboxId"]}|$token|${body.size}".encodeToByteArray(),
                                ContentType.Text.Plain,
                                HttpStatusCode.Created
                            )
                        }
                        get("/v1/mailboxes/{mailboxId}/envelopes") {
                            call.respondBytes(ByteArray(LARGE_ANSWER_BYTES) { (it % 251).toByte() }, ContentType.Application.Json)
                        }
                    }
                }
            }
            routing { installCircuitRoute(relayA) }
            block()
        }

    private suspend fun ApplicationTestBuilder.circuit(block: suspend TestCircuit.() -> Unit) {
        createClient { install(ClientWebSockets) }.webSocket("/v1/circuit") { TestCircuit(this).block() }
    }

    /** Plays the app: builds the circuit hop by hop and layers its cells. */
    private class TestCircuit(
        val session: DefaultClientWebSocketSession
    ) {
        private val hops = mutableListOf<Hop>()

        suspend fun create(relay: X25519KeyPair) {
            val ephemeral = CircuitHandshake.initiate()
            session.send(Frame.Binary(true, CircuitCell(CIRCUIT_ID, CircuitCell.CREATE, ephemeral.publicKey).encode()))
            val created = receiveCell()
            assertEquals(CircuitCell.CREATED, created.command)
            hops += Hop(assertNotNull(CircuitHandshake.complete(ephemeral, relay.publicKey, created.body.copyOf(64))))
        }

        suspend fun extend(
            url: String,
            relay: X25519KeyPair
        ): Int {
            val ephemeral = CircuitHandshake.initiate()
            val answer = exchange(RelayPayload(RelayCommand.EXTEND, 0, ExtendRequest(url, ephemeral.publicKey).encode()))
            if (answer.command == RelayCommand.EXTENDED) {
                hops += Hop(assertNotNull(CircuitHandshake.complete(ephemeral, relay.publicKey, answer.data)))
            }
            return answer.command
        }

        suspend fun ping(data: ByteArray): ByteArray {
            val answer = exchange(RelayPayload(RelayCommand.PING, 1, data))
            assertEquals(RelayCommand.PONG, answer.command)
            return answer.data
        }

        /** Sends [request] in parts on [stream]; the command that ended the answer and the answer's bytes. */
        suspend fun request(
            stream: Int,
            request: CircuitHttpRequest
        ): Pair<Int, ByteArray> {
            val bytes = request.encode()
            val parts = (bytes.indices step RelayPayload.MAXIMUM_DATA_BYTES).map { start -> bytes.copyOfRange(start, minOf(start + RelayPayload.MAXIMUM_DATA_BYTES, bytes.size)) }
            parts.dropLast(1).forEach { part -> send(RelayPayload(RelayCommand.STREAM_DATA, stream, part)) }
            send(RelayPayload(RelayCommand.STREAM_END, stream, parts.last()))
            var answer = ByteArray(0)
            while (true) {
                val part = receive()
                assertEquals(stream, part.stream)
                answer += part.data
                if (part.command != RelayCommand.STREAM_DATA) return part.command to answer
            }
        }

        suspend fun receiveCell(): CircuitCell = assertNotNull(CircuitCell.decode((session.incoming.receive() as Frame.Binary).readBytes()))

        private suspend fun exchange(request: RelayPayload): RelayPayload {
            send(request)
            return receive()
        }

        private suspend fun send(request: RelayPayload) {
            val last = hops.last()
            var body = CircuitLayer.seal(last.keys.forward, last.forwardCount++, request.encode())
            hops.dropLast(1).asReversed().forEach { hop -> body = CircuitLayer.stream(hop.keys.forward, hop.forwardCount++, body) }
            session.send(Frame.Binary(true, CircuitCell(CIRCUIT_ID, CircuitCell.RELAY, body).encode()))
        }

        private suspend fun receive(): RelayPayload {
            val last = hops.last()
            var answer = receiveCell().body
            hops.dropLast(1).forEach { hop -> answer = CircuitLayer.stream(hop.keys.backward, hop.backwardCount++, answer) }
            val opened = assertNotNull(CircuitLayer.open(last.keys.backward, last.backwardCount++, answer))
            return assertNotNull(RelayPayload.decode(opened))
        }

        private class Hop(
            val keys: CircuitHopKeys
        ) {
            var forwardCount = 0L
            var backwardCount = 0L
        }
    }

    private fun status(answer: ByteArray): Int = ((answer[0].toInt() and 0xFF) shl 8) or (answer[1].toInt() and 0xFF)

    private fun body(answer: ByteArray): ByteArray = answer.copyOfRange(3 + (answer[2].toInt() and 0xFF), answer.size)

    private companion object {
        const val LARGE_ANSWER_BYTES = 3_000
        const val CIRCUIT_ID = 7
        val PUBLIC_ADDRESS: InetAddress = InetAddress.getByAddress(byteArrayOf(8, 8, 8, 8))
    }
}
