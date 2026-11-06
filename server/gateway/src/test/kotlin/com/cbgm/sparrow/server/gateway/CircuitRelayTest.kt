package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.security.CircuitHandshake
import com.cbgm.sparrow.server.security.CircuitHopKeys
import com.cbgm.sparrow.server.security.CircuitLayer
import com.cbgm.sparrow.server.security.X25519KeyPair
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.application.install
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
            val relayA = CircuitRelay(keysA, onward, policy)
            val relayB = CircuitRelay(keysB, onward, policy)
            externalServices {
                hosts("ws://relay-b") {
                    this.install(ServerWebSockets)
                    this.routing { installCircuitRoute(relayB) }
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

        suspend fun receiveCell(): CircuitCell = assertNotNull(CircuitCell.decode((session.incoming.receive() as Frame.Binary).readBytes()))

        private suspend fun exchange(request: RelayPayload): RelayPayload {
            val last = hops.last()
            var body = CircuitLayer.seal(last.keys.forward, last.forwardCount++, request.encode())
            hops.dropLast(1).asReversed().forEach { hop -> body = CircuitLayer.stream(hop.keys.forward, hop.forwardCount++, body) }
            session.send(Frame.Binary(true, CircuitCell(CIRCUIT_ID, CircuitCell.RELAY, body).encode()))

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

    private companion object {
        const val CIRCUIT_ID = 7
        val PUBLIC_ADDRESS: InetAddress = InetAddress.getByAddress(byteArrayOf(8, 8, 8, 8))
    }
}
