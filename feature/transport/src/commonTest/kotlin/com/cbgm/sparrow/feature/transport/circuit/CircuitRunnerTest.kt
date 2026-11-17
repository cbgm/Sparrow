package com.cbgm.sparrow.feature.transport.circuit

import com.cbgm.sparrow.feature.transport.discovery.CachedNodeDirectory
import com.cbgm.sparrow.feature.transport.discovery.NodeCapability
import com.cbgm.sparrow.feature.transport.discovery.NodeDirectory
import com.cbgm.sparrow.feature.transport.discovery.NodeDirectoryCache
import com.cbgm.sparrow.feature.transport.discovery.SignedNodeDirectory
import com.cbgm.sparrow.feature.transport.discovery.SparrowNodeDescriptor
import com.cbgm.sparrow.feature.transport.gateway.codec.createGatewayJson
import com.cbgm.sparrow.feature.transport.relay.RelaySelector
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * The runner's privacy rules for several circuits, over simulated relays: the circuits end at different operators, and
 * a contact's requests never cross to a circuit of another operator, which would then see mailboxes of two circuits.
 */
class CircuitRunnerTest {
    private val json = createGatewayJson()
    private val cipher = ReadableCircuitCipher()

    @Test
    fun circuitsBuiltAtTheSameTimeEndAtDifferentOperators() =
        runTest {
            val runner = runner(backgroundScope, operators = mapOf("b" to "Operator B", "c" to "Operator C"))
            backgroundScope.launch { runner.run() }
            // The runner takes the number of circuits from the setting, which the slots of the keys depend on.
            runCurrent()

            val (first, second) = awaitBothCircuits(runner)

            assertNotEquals(first.sessionOperator, second.sessionOperator)
        }

    @Test
    fun whileItsCircuitIsRebuiltAContactsRequestsDoNotCrossToAnotherOperator() =
        runTest {
            val runner = runner(backgroundScope, operators = mapOf("b" to "Operator B", "c" to "Operator C"))
            backgroundScope.launch { runner.run() }
            // The runner takes the number of circuits from the setting, which the slots of the keys depend on.
            runCurrent()
            val key = keyFor(runner, slot = 0)
            val otherKey = keyFor(runner, slot = 1)
            awaitBothCircuits(runner)
            val own = assertNotNull(runner.currentCircuit(key))

            own.close()
            runCurrent()

            assertNull(runner.currentCircuit(key))
            assertNotNull(runner.currentCircuit(otherKey))
            // A request about no mailbox in particular may still use the open circuit.
            assertNotNull(runner.currentCircuit())

            advanceTimeBy(REBUILD_MILLISECONDS)
            runCurrent()
            assertEquals(own.sessionOperator, runner.currentCircuit(key)?.sessionOperator)
        }

    @Test
    fun withASingleOperatorTheOtherCircuitStandsInWhileOneIsRebuilt() =
        runTest {
            val runner = runner(backgroundScope, operators = mapOf("b" to "Operator X", "c" to "Operator X"))
            backgroundScope.launch { runner.run() }
            // The runner takes the number of circuits from the setting, which the slots of the keys depend on.
            runCurrent()
            val key = keyFor(runner, slot = 0)
            val otherKey = keyFor(runner, slot = 1)
            awaitBothCircuits(runner)
            val own = assertNotNull(runner.currentCircuit(key))
            val other = assertNotNull(runner.currentCircuit(otherKey))
            assertEquals(own.sessionOperator, other.sessionOperator)

            own.close()
            runCurrent()

            // That operator has seen these mailboxes already, so nothing new is linked.
            assertSame(other, runner.currentCircuit(key))
        }

    private fun TestScope.awaitBothCircuits(runner: CircuitRunner): Pair<Circuit, Circuit> {
        val keys = keyFor(runner, slot = 0) to keyFor(runner, slot = 1)
        repeat(MAXIMUM_STEPS) {
            val first = runner.currentCircuit(keys.first)
            val second = runner.currentCircuit(keys.second)
            if (first != null && second != null && first !== second) return first to second
            advanceTimeBy(STEP_MILLISECONDS)
            runCurrent()
        }
        error("The circuits were not built")
    }

    private fun keyFor(
        runner: CircuitRunner,
        slot: Int
    ): String = generateSequence(0) { it + 1 }.map { "contact-$it" }.first { key -> runner.slotOf(key) == slot }

    private fun runner(
        scope: CoroutineScope,
        operators: Map<String, String>
    ): CircuitRunner {
        val nodes = listOf(node("own", "Home")) + operators.map { (name, operator) -> node(name, operator) }
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
        return CircuitRunner(
            httpClient = HttpClient(MockEngine { respond("") }),
            cipher = cipher,
            selector = RelaySelector(cache, json, preferredNodeStore = null, connectedNodeId = { "own" }, now = { 1L }),
            hopCountStore = TwoHops,
            circuitCountStore = TwoCircuits,
            openSession = { _ -> SimulatedRelays(scope, cipher).session }
        )
    }

    private fun node(
        name: String,
        operator: String
    ) = SparrowNodeDescriptor(
        nodeId = name,
        clientEndpoint = "wss://$name.test/v1/gateway",
        mailboxEndpoint = "https://$name.test",
        identityPublicKey = byteArrayOf(1),
        protocolVersions = setOf(1),
        capabilities = NodeCapability.entries.toSet(),
        validUntilEpochMilliseconds = 10L,
        relayKey = ByteArray(32) { name.first().code.toByte() },
        operator = operator,
        signature = byteArrayOf(1)
    )

    private object TwoHops : CircuitHopCountStore {
        override fun observe(): Flow<Int> = flowOf(2)

        override suspend fun set(hopCount: Int) = Unit
    }

    private object TwoCircuits : CircuitCountStore {
        override fun observe(): Flow<Int> = flowOf(2)

        override suspend fun set(circuitCount: Int) = Unit
    }

    private companion object {
        const val STEP_MILLISECONDS = 100L
        const val MAXIMUM_STEPS = 100

        /** The next ping notices the closed circuit (30 s), and it is rebuilt after the first retry delay (5 s). */
        const val REBUILD_MILLISECONDS = 40_000L
    }
}
