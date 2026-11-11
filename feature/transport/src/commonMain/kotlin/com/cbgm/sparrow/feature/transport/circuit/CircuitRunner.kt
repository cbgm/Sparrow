package com.cbgm.sparrow.feature.transport.circuit

import com.cbgm.sparrow.core.crypto.circuit.CircuitCipher
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.transport.relay.RelaySelector
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/** Where the app's circuit stands; [Connected.latencyMilliseconds] is the last measured round trip. */
sealed interface CircuitStatus {
    data object Building : CircuitStatus

    /**
     * [circuitsUp] of the [requestedCircuitCount] circuits are open and end at [sessionOperators] different operators;
     * the hops and round trip are the best one's.
     */
    data class Connected(
        val hopCount: Int,
        val requestedHopCount: Int,
        val latencyMilliseconds: Long,
        val circuitsUp: Int = 1,
        val requestedCircuitCount: Int = 1,
        val sessionOperators: Int = 1
    ) : CircuitStatus

    data class Unavailable(
        val requestedHopCount: Int
    ) : CircuitStatus
}

/**
 * Keeps the chosen number of circuits open while it runs ([run], cancelled when the app leaves the foreground), each
 * built with as many of the chosen hops as there are operators running relaying nodes and ending at a session node of
 * a different operator when there are enough operators; each is measured with a ping every
 * [pingIntervalMilliseconds], rebuilt when it fails or a setting changes. Requests are spread across them by a key
 * ([currentCircuit]) and never cross to a circuit of another operator, so no operator sees all of the device's
 * mailboxes. A hop that fails is left out for a while ([RelaySelector.recordFailure]).
 */
class CircuitRunner internal constructor(
    private val httpClient: HttpClient,
    private val cipher: CircuitCipher,
    private val selector: RelaySelector,
    private val hopCountStore: CircuitHopCountStore,
    private val circuitCountStore: CircuitCountStore,
    private val pingIntervalMilliseconds: Long = DEFAULT_PING_INTERVAL_MILLISECONDS
) {
    private val logger = SparrowLog.withTag("CircuitRunner")
    private val mutableStatus = MutableStateFlow<CircuitStatus>(CircuitStatus.Building)
    val status: StateFlow<CircuitStatus> = mutableStatus.asStateFlow()
    private val circuits = List(CircuitCount.MAXIMUM) { MutableStateFlow<Circuit?>(null) }
    private val slotStatuses = List(CircuitCount.MAXIMUM) { MutableStateFlow<CircuitStatus>(CircuitStatus.Building) }

    /** The operator each circuit last ended at: its mailboxes are known there, so they may go there while it is rebuilt. */
    private val sessionOperators = List(CircuitCount.MAXIMUM) { MutableStateFlow<String?>(null) }
    private val activeCount = MutableStateFlow(1)

    /** Which circuit requests with [key] (a mailbox or contact) use, as long as the number of circuits stays. */
    internal fun slotOf(key: String): Int = (key.hashCode() and Int.MAX_VALUE) % activeCount.value

    /**
     * An open circuit through at least [minimumHopCount] hops, or null while there is none (being built, failed, too
     * few relaying nodes, or the app is in the background). Without [key] the best one. With [key] the one for it, or
     * while that is rebuilt another one ending at the same operator, which has seen these mailboxes already; never one
     * of another operator, which would then see mailboxes of two circuits together.
     */
    internal fun currentCircuit(
        key: String? = null,
        minimumHopCount: Int = 1
    ): Circuit? =
        key
            ?.let { circuitFor(slotOf(it), minimumHopCount) }
            ?: usableCircuits(minimumHopCount).takeIf { key == null }?.maxByOrNull { circuit -> circuit.nodeIds.size }

    /** Whether any circuit through at least [minimumHopCount] hops is open. */
    internal fun hasCircuit(minimumHopCount: Int = 1): Boolean = usableCircuits(minimumHopCount).isNotEmpty()

    /** Waits up to [timeoutMilliseconds] for an open circuit through at least [minimumHopCount] hops, for [key] as in [currentCircuit]. */
    internal suspend fun awaitCircuit(
        timeoutMilliseconds: Long,
        minimumHopCount: Int = 1,
        key: String? = null
    ): Circuit? =
        withTimeoutOrNull(timeoutMilliseconds.milliseconds) {
            combine(circuits) { open -> open.toList() }.first { currentCircuit(key, minimumHopCount) != null }
            currentCircuit(key, minimumHopCount)
        }

    private fun circuitFor(
        slot: Int,
        minimumHopCount: Int
    ): Circuit? =
        circuits[slot].value?.takeIf { circuit -> circuit.isUsable(minimumHopCount) }
            ?: sessionOperators[slot].value?.let { operator ->
                usableCircuits(minimumHopCount).firstOrNull { circuit -> circuit.sessionOperator == operator }
            }

    /**
     * Builds a circuit of its own for one file transfer, through the chosen hops and ending at a session node the
     * other circuits do not use when there is one, runs [block] on it and closes it: a large transfer does not hold
     * back messages, and no node can link it to the device's mailboxes. Fails with [ProtectedRouteMissingException]
     * when it gets fewer than [minimumHopCount] hops.
     */
    internal suspend fun <T> withOwnCircuit(
        minimumHopCount: Int,
        block: suspend (Circuit) -> T
    ): T {
        val hopCount = hopCountStore.observe().first()
        val circuit = build("for a transfer", hopCount, otherSessionOperators(slot = NO_SLOT), TRANSFER_ANSWER_TIMEOUT_MILLISECONDS)
        return try {
            if (circuit.nodeIds.size < minimumHopCount) throw ProtectedRouteMissingException()
            coroutineScope {
                val reading = launch { circuit.readAnswers() }
                try {
                    block(circuit)
                } finally {
                    reading.cancel()
                }
            }
        } finally {
            circuit.close()
        }
    }

    private fun usableCircuits(minimumHopCount: Int): List<Circuit> =
        circuits.take(activeCount.value).mapNotNull { slot -> slot.value?.takeIf { circuit -> circuit.isUsable(minimumHopCount) } }

    private fun Circuit.isUsable(minimumHopCount: Int): Boolean = !isClosed && nodeIds.size >= minimumHopCount

    suspend fun run() {
        combine(hopCountStore.observe(), circuitCountStore.observe()) { hopCount, circuitCount -> hopCount to circuitCount }
            .collectLatest { (hopCount, circuitCount) ->
                activeCount.value = circuitCount
                slotStatuses.forEach { slot -> slot.value = CircuitStatus.Building }
                // Another number of circuits spreads the mailboxes anew.
                sessionOperators.forEach { operator -> operator.value = null }
                publishStatus(hopCount)
                coroutineScope { repeat(circuitCount) { slot -> launch { keepCircuit(slot, hopCount) } } }
            }
    }

    private suspend fun keepCircuit(
        slot: Int,
        hopCount: Int
    ) {
        var retryDelay = FIRST_RETRY_MILLISECONDS
        var next: Circuit? = null
        while (currentCoroutineContext().isActive) {
            if (next == null) setStatus(slot, hopCount, CircuitStatus.Building)
            try {
                val circuit = next ?: build("${slot + 1}", hopCount, otherSessionOperators(slot))
                next = null
                retryDelay = FIRST_RETRY_MILLISECONDS
                sessionOperators[slot].value = circuit.sessionOperator
                circuits[slot].value = circuit
                try {
                    next =
                        coroutineScope {
                            val reading = launch { circuit.readAnswers() }
                            measure(slot, circuit, hopCount).also { reading.cancel() }
                        }
                    // The fuller circuit takes over before the old one closes, so requests always find one.
                    sessionOperators[slot].value = next.sessionOperator
                    circuits[slot].value = next
                } finally {
                    circuits[slot].compareAndSet(circuit, null)
                    circuit.close()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                (failure as? CircuitHopFailedException)?.let { hopFailure -> selector.recordFailure(hopFailure.nodeId) }
                logger.warn { "Circuit ${slot + 1} failed (${failure.message ?: failure::class.simpleName}); rebuilding in ${retryDelay / 1_000} s" }
                setStatus(slot, hopCount, CircuitStatus.Unavailable(hopCount))
                delay(retryDelay.milliseconds)
                retryDelay = (retryDelay * 2).coerceAtMost(MAXIMUM_RETRY_MILLISECONDS)
            }
        }
    }

    private fun setStatus(
        slot: Int,
        hopCount: Int,
        status: CircuitStatus
    ) {
        slotStatuses[slot].value = status
        publishStatus(hopCount)
    }

    /**
     * The best open circuit's hops and round trip, how many are open and at how many operators they end; building
     * while none is but one is being built.
     */
    private fun publishStatus(hopCount: Int) {
        val active = slotStatuses.take(activeCount.value).map { slot -> slot.value }
        val connected = active.filterIsInstance<CircuitStatus.Connected>()
        val operators =
            (0 until activeCount.value)
                .filter { slot -> slotStatuses[slot].value is CircuitStatus.Connected }
                .mapNotNullTo(HashSet()) { slot -> sessionOperators[slot].value }
        mutableStatus.value =
            connected
                .maxWithOrNull(compareBy<CircuitStatus.Connected> { it.hopCount }.thenByDescending { it.latencyMilliseconds })
                ?.copy(circuitsUp = connected.size, requestedCircuitCount = active.size, sessionOperators = operators.size)
                ?: active.firstOrNull { it is CircuitStatus.Building }
                ?: CircuitStatus.Unavailable(hopCount)
    }

    /** The operators the other circuits end at, so this one ends at another operator when it can. */
    private fun otherSessionOperators(slot: Int): Set<String> =
        circuits.take(activeCount.value).filterIndexed { index, _ -> index != slot }.mapNotNullTo(HashSet()) { it.value?.sessionOperator }

    private suspend fun build(
        name: String,
        hopCount: Int,
        avoidLastOperators: Set<String>,
        answerTimeoutMilliseconds: Long? = null
    ): Circuit {
        val route = selector.circuitRoute(hopCount, avoidLastOperators = avoidLastOperators)
        check(route.isNotEmpty()) { "no relaying node known yet" }
        val first = route.first()
        val session =
            try {
                httpClient.webSocketSession(first.circuitUrl)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (unreachable: Exception) {
                throw CircuitHopFailedException(first.nodeId, "not reachable: ${unreachable::class.simpleName}")
            }
        val circuit = answerTimeoutMilliseconds?.let { timeout -> Circuit(session, cipher, timeout) } ?: Circuit(session, cipher)
        try {
            circuit.create(first)
            route.drop(1).forEach { hop -> circuit.extend(hop) }
        } catch (failure: Exception) {
            circuit.close()
            throw failure
        }
        selector.recordSuccess(route)
        logger.info {
            "Circuit $name built through ${route.size} hop(s): ${circuit.nodeIds.joinToString(" -> ") { it.take(8) }}" +
                if (route.size < hopCount) " (fewer than the chosen $hopCount: not enough relaying nodes)" else ""
        }
        return circuit
    }

    /** Pings [circuit] until it fails, or until a fuller circuit was built, which it returns. */
    private suspend fun measure(
        slot: Int,
        circuit: Circuit,
        hopCount: Int
    ): Circuit {
        while (true) {
            val latency = circuit.ping()
            logger.debug { "Circuit ${slot + 1} round trip: $latency ms" }
            setStatus(slot, hopCount, CircuitStatus.Connected(circuit.nodeIds.size, hopCount, latency))
            delay(pingIntervalMilliseconds.milliseconds)
            fuller(slot, circuit, hopCount)?.let { fuller -> return fuller }
        }
    }

    /**
     * A circuit through more hops than [circuit] once relaying nodes that were missing are back; [circuit] keeps
     * working while it is built, and stays when that fails.
     */
    private suspend fun fuller(
        slot: Int,
        circuit: Circuit,
        hopCount: Int
    ): Circuit? {
        val hops = circuit.nodeIds.size
        if (hops >= hopCount || selector.circuitRoute(hopCount).size <= hops) return null
        return try {
            val built = build("${slot + 1}", hopCount, otherSessionOperators(slot))
            if (built.nodeIds.size > hops) {
                built
            } else {
                built.close()
                null
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            (failure as? CircuitHopFailedException)?.let { hopFailure -> selector.recordFailure(hopFailure.nodeId) }
            logger.info { "Circuit through more hops not built yet (${failure.message ?: failure::class.simpleName}); keeping $hops hop(s)" }
            null
        }
    }

    private companion object {
        const val DEFAULT_PING_INTERVAL_MILLISECONDS = 30_000L
        const val FIRST_RETRY_MILLISECONDS = 5_000L
        const val MAXIMUM_RETRY_MILLISECONDS = 60_000L
        const val NO_SLOT = -1

        /** A chunk of a file takes about a thousand cells through every hop; on a slow network that takes a while. */
        const val TRANSFER_ANSWER_TIMEOUT_MILLISECONDS = 120_000L
    }
}
