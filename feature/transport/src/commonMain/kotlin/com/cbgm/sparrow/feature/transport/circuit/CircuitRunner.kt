package com.cbgm.sparrow.feature.transport.circuit

import com.cbgm.sparrow.core.crypto.circuit.CircuitCipher
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.transport.relay.RelaySelector
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

/** Where the app's circuit stands; [Connected.latencyMilliseconds] is the last measured round trip. */
sealed interface CircuitStatus {
    data object Building : CircuitStatus

    data class Connected(
        val hopCount: Int,
        val requestedHopCount: Int,
        val latencyMilliseconds: Long
    ) : CircuitStatus

    data class Unavailable(
        val requestedHopCount: Int
    ) : CircuitStatus
}

/**
 * Keeps one circuit open while it runs ([run], cancelled when the app leaves the foreground): built with as many of
 * the chosen hops as there are relaying nodes, measured with a ping every [pingIntervalMilliseconds], rebuilt when it
 * fails or the hop count changes. A hop that fails is left out for a while ([RelaySelector.recordFailure]).
 */
class CircuitRunner internal constructor(
    private val httpClient: HttpClient,
    private val cipher: CircuitCipher,
    private val selector: RelaySelector,
    private val hopCountStore: CircuitHopCountStore,
    private val pingIntervalMilliseconds: Long = DEFAULT_PING_INTERVAL_MILLISECONDS
) {
    private val logger = SparrowLog.withTag("CircuitRunner")
    private val mutableStatus = MutableStateFlow<CircuitStatus>(CircuitStatus.Building)
    val status: StateFlow<CircuitStatus> = mutableStatus.asStateFlow()

    suspend fun run() {
        hopCountStore.observe().collectLatest { hopCount -> keepCircuit(hopCount) }
    }

    private suspend fun keepCircuit(hopCount: Int) {
        var retryDelay = FIRST_RETRY_MILLISECONDS
        while (currentCoroutineContext().isActive) {
            mutableStatus.value = CircuitStatus.Building
            try {
                val circuit = build(hopCount)
                retryDelay = FIRST_RETRY_MILLISECONDS
                try {
                    measure(circuit, hopCount)
                } finally {
                    circuit.close()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                (failure as? CircuitHopFailedException)?.let { hopFailure -> selector.recordFailure(hopFailure.nodeId) }
                logger.warn { "Circuit failed (${failure.message ?: failure::class.simpleName}); rebuilding in ${retryDelay / 1_000} s" }
                mutableStatus.value = CircuitStatus.Unavailable(hopCount)
                delay(retryDelay.milliseconds)
                retryDelay = (retryDelay * 2).coerceAtMost(MAXIMUM_RETRY_MILLISECONDS)
            }
        }
    }

    private suspend fun build(hopCount: Int): Circuit {
        val route = selector.circuitRoute(hopCount)
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
        val circuit = Circuit(session, cipher)
        try {
            circuit.create(first)
            route.drop(1).forEach { hop -> circuit.extend(hop) }
        } catch (failure: Exception) {
            circuit.close()
            throw failure
        }
        selector.recordSuccess(route)
        logger.info {
            "Circuit built through ${route.size} hop(s): ${circuit.nodeIds.joinToString(" -> ") { it.take(8) }}" +
                if (route.size < hopCount) " (fewer than the chosen $hopCount: not enough relaying nodes)" else ""
        }
        return circuit
    }

    private suspend fun measure(
        circuit: Circuit,
        hopCount: Int
    ) {
        while (currentCoroutineContext().isActive) {
            val latency = circuit.ping()
            logger.debug { "Circuit round trip: $latency ms" }
            mutableStatus.value = CircuitStatus.Connected(circuit.nodeIds.size, hopCount, latency)
            delay(pingIntervalMilliseconds.milliseconds)
        }
    }

    private companion object {
        const val DEFAULT_PING_INTERVAL_MILLISECONDS = 30_000L
        const val FIRST_RETRY_MILLISECONDS = 5_000L
        const val MAXIMUM_RETRY_MILLISECONDS = 60_000L
    }
}
