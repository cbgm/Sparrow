package com.cbgm.sparrow.feature.transport.circuit

import com.cbgm.sparrow.core.crypto.circuit.CircuitCipher
import com.cbgm.sparrow.core.crypto.circuit.CircuitEphemeralKey
import com.cbgm.sparrow.core.crypto.circuit.CircuitHopKeys
import com.cbgm.sparrow.feature.transport.relay.RelayHop
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketExtension
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CircuitTest {
    private val cipher = ReadableCircuitCipher()

    @Test
    fun aThreeHopCircuitIsBuiltHopByHopAndAnswersPings() =
        runTest {
            val relays = SimulatedRelays(this, cipher)
            val circuit = Circuit(relays.session, cipher, now = { 0L })

            circuit.create(hop("a", 1))
            circuit.extend(hop("b", 2))
            circuit.extend(hop("c", 3))
            circuit.ping()
            circuit.ping()

            assertEquals(listOf("a", "b", "c"), circuit.nodeIds)
            assertEquals(listOf("wss://b.test/v1/circuit", "wss://c.test/v1/circuit"), relays.extendedTo)
            assertEquals(2, relays.pingsAtLastHop)
            relays.stop()
        }

    @Test
    fun aHopThatCannotBeReachedIsNamed() =
        runTest {
            val relays = SimulatedRelays(this, cipher, unreachable = setOf("wss://down.test/v1/circuit"))
            val circuit = Circuit(relays.session, cipher, now = { 0L })
            circuit.create(hop("a", 1))

            val failure = assertFailsWith<CircuitHopFailedException> { circuit.extend(hop("down", 2)) }

            assertEquals("down", failure.nodeId)
            relays.stop()
        }

    private fun hop(
        name: String,
        key: Int
    ) = RelayHop(name, "https://$name.test/v1/relay", "wss://$name.test/v1/circuit", ByteArray(32) { key.toByte() })

    /** Plays the relays of a circuit the way the gateway's CircuitRelay does, with the readable test cipher. */
    private class SimulatedRelays(
        scope: CoroutineScope,
        private val cipher: ReadableCircuitCipher,
        private val unreachable: Set<String> = emptySet()
    ) {
        private val toApp = Channel<Frame>(Channel.UNLIMITED)
        private val fromApp = Channel<Frame>(Channel.UNLIMITED)
        private val hops = mutableListOf<Hop>()
        val extendedTo = mutableListOf<String>()
        var pingsAtLastHop = 0
        val session: WebSocketSession = ChannelSession(toApp, fromApp)
        private val job: Job =
            scope.launch {
                for (frame in fromApp) {
                    val cell = CircuitCell.decode((frame as Frame.Binary).readBytes()) ?: continue
                    when (cell.command) {
                        CircuitCell.CREATE -> created(cell.body.copyOf(32))
                        CircuitCell.RELAY -> relay(cell.body)
                        else -> Unit
                    }
                }
            }

        fun stop() {
            job.cancel()
        }

        private suspend fun created(ephemeral: ByteArray) {
            val hop = Hop(ephemeral.first().toInt())
            hops += hop
            toApp.send(Frame.Binary(true, CircuitCell(1, CircuitCell.CREATED, hop.reply()).encode()))
        }

        private suspend fun relay(body: ByteArray) {
            val inner = hops.dropLast(1)
            val last = hops.last()
            var layered = body
            inner.forEach { hop -> layered = cipher.stream(hop.keys.forward, hop.forwardCount++, layered) }
            val request = RelayPayload.decode(cipher.open(last.keys.forward, last.forwardCount++, layered)!!)!!
            val answer =
                when (request.command) {
                    RelayCommand.EXTEND -> extend(request.data)
                    RelayCommand.PING -> RelayPayload(RelayCommand.PONG, request.stream, request.data).also { pingsAtLastHop++ }
                    else -> error("Unexpected command ${request.command}")
                }
            var back = cipher.seal(last.keys.backward, last.backwardCount++, answer.encode())
            inner.asReversed().forEach { hop -> back = cipher.stream(hop.keys.backward, hop.backwardCount++, back) }
            toApp.send(Frame.Binary(true, CircuitCell(1, CircuitCell.RELAY, back).encode()))
        }

        private fun extend(data: ByteArray): RelayPayload {
            val urlLength = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            val url = data.copyOfRange(2, 2 + urlLength).decodeToString()
            if (url in unreachable) return RelayPayload(RelayCommand.EXTEND_FAILED, 0, ByteArray(0))
            extendedTo += url
            // The reply of the new hop is created before the last hop changes, as the gateway attaches it.
            val next = Hop(data[2 + urlLength].toInt())
            val reply = next.reply()
            return RelayPayload(RelayCommand.EXTENDED, 0, reply).also { hops += next }
        }

        private class Hop(
            seed: Int
        ) {
            val keys = CircuitHopKeys(ByteArray(32) { seed.toByte() }, ByteArray(32) { (seed + 100).toByte() })
            var forwardCount = 0L
            var backwardCount = 0L

            fun reply(): ByteArray = keys.forward + keys.backward
        }
    }

    private class ChannelSession(
        override val incoming: ReceiveChannel<Frame>,
        override val outgoing: SendChannel<Frame>
    ) : WebSocketSession {
        override val coroutineContext: CoroutineContext = Job()
        override var masking: Boolean = false
        override var maxFrameSize: Long = Long.MAX_VALUE
        override val extensions: List<WebSocketExtension<*>> = emptyList()

        override suspend fun flush() = Unit

        @Deprecated("Use cancel() instead.", level = DeprecationLevel.ERROR)
        override fun terminate() = Unit
    }
}

/**
 * Stands in for libsodium (device tests cover the real cipher): the handshake reply carries the hop keys, a layer is
 * an XOR with the key's first byte and the counter, and a seal adds a 16-byte tag made of that byte.
 */
internal class ReadableCircuitCipher : CircuitCipher {
    override suspend fun newEphemeralKey(): CircuitEphemeralKey = CircuitEphemeralKey(ByteArray(32), ByteArray(32) { nextSeed.toByte() }).also { nextSeed++ }

    private var nextSeed = 2

    override suspend fun completeHandshake(
        ephemeralKey: CircuitEphemeralKey,
        relayPublicKey: ByteArray,
        reply: ByteArray
    ): CircuitHopKeys? = reply.takeIf { it.size == 64 }?.let { CircuitHopKeys(it.copyOf(32), it.copyOfRange(32, 64)) }

    override suspend fun stream(
        key: ByteArray,
        counter: Long,
        data: ByteArray
    ): ByteArray = ByteArray(data.size) { index -> (data[index].toInt() xor (key.first() + counter.toInt())).toByte() }

    override suspend fun seal(
        key: ByteArray,
        counter: Long,
        data: ByteArray
    ): ByteArray = stream(key, counter, data) + ByteArray(16) { key.first() }

    override suspend fun open(
        key: ByteArray,
        counter: Long,
        sealed: ByteArray
    ): ByteArray? =
        sealed
            .takeIf { it.size >= 16 && it.copyOfRange(it.size - 16, it.size).all { byte -> byte == key.first() } }
            ?.let { stream(key, counter, it.copyOf(it.size - 16)) }
}
