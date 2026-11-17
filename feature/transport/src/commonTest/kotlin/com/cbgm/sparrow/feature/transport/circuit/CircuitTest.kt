package com.cbgm.sparrow.feature.transport.circuit

import com.cbgm.sparrow.core.crypto.circuit.CircuitCipher
import com.cbgm.sparrow.core.crypto.circuit.CircuitEphemeralKey
import com.cbgm.sparrow.core.crypto.circuit.CircuitHopKeys
import com.cbgm.sparrow.feature.transport.relay.RelayHop
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
            backgroundScope.launch { circuit.readAnswers() }
            circuit.ping()
            circuit.ping()

            assertEquals(listOf("a", "b", "c"), circuit.nodeIds)
            assertEquals(listOf("wss://b.test/v1/circuit", "wss://c.test/v1/circuit"), relays.extendedTo)
            assertEquals(2, relays.pingsAtLastHop)
            relays.stop()
        }

    @Test
    fun aMailboxRequestAndItsAnswerSpanManyCells() =
        runTest {
            val relays = SimulatedRelays(this, cipher)
            val circuit = Circuit(relays.session, cipher, now = { 0L })
            circuit.create(hop("a", 1))
            circuit.extend(hop("b", 2))
            backgroundScope.launch { circuit.readAnswers() }

            val answer =
                circuit.request(
                    CircuitHttpRequest("POST", "https://box.test/v1/mailboxes/m/envelopes", "send-capability", "application/json", ByteArray(2_500) { 7 })
                )

            assertEquals("POST https://box.test/v1/mailboxes/m/envelopes send-capability 2500", relays.lastRequest)
            assertEquals(201, answer.status)
            assertEquals("text/plain", answer.contentType)
            assertContentEquals(ByteArray(3_000) { (it % 251).toByte() }, answer.body)
            relays.stop()
        }

    @Test
    fun aRefusedRequestLeavesTheCircuitUsable() =
        runTest {
            val relays = SimulatedRelays(this, cipher)
            val circuit = Circuit(relays.session, cipher, now = { 0L })
            circuit.create(hop("a", 1))
            backgroundScope.launch { circuit.readAnswers() }

            assertFailsWith<CircuitRequestRefusedException> {
                circuit.request(CircuitHttpRequest("GET", "https://box.test/v1/routes/x", null, null, ByteArray(0)))
            }
            circuit.ping()
            relays.stop()
        }

    @Test
    fun aWaitingRequestDoesNotHoldBackOthers() =
        runTest {
            val relays = SimulatedRelays(this, cipher)
            val circuit = Circuit(relays.session, cipher, now = { 0L })
            circuit.create(hop("a", 1))
            circuit.extend(hop("b", 2))
            backgroundScope.launch { circuit.readAnswers() }

            // The simulated session node answers the wait only after the ping that follows it got through.
            val waiting = async(start = CoroutineStart.UNDISPATCHED) { circuit.request(CircuitHttpRequest("POST", "https://box.test/v1/mailboxes/wait", null, "application/json", ByteArray(10))) }
            circuit.ping()

            assertEquals(200, waiting.await().status)
            relays.stop()
        }

    @Test
    fun aFailingCircuitFailsTheRequestsOnTheWay() =
        runTest {
            val relays = SimulatedRelays(this, cipher)
            val circuit = Circuit(relays.session, cipher, now = { 0L })
            circuit.create(hop("a", 1))
            val reading = backgroundScope.launch { runCatching { circuit.readAnswers() } }

            val waiting = async(start = CoroutineStart.UNDISPATCHED) { runCatching { circuit.request(CircuitHttpRequest("POST", "https://box.test/v1/mailboxes/wait", null, "application/json", ByteArray(10))) } }
            relays.destroy()
            reading.join()

            assertTrue(waiting.await().isFailure)
            assertTrue(circuit.isClosed)
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
