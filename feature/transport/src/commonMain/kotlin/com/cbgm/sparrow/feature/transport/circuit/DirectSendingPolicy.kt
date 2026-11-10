package com.cbgm.sparrow.feature.transport.circuit

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Nothing left the device: sending directly is switched off and no circuit through enough hops is up. */
class ProtectedRouteMissingException :
    IllegalStateException(
        "No circuit through at least ${CircuitHopCount.PROTECTING} hops; sending directly is switched off"
    )

/** True when this failure, or one behind it, is [ProtectedRouteMissingException]: the user's choice held it back. */
fun Throwable.isWaitingForRoute(): Boolean = generateSequence(this) { failure -> failure.cause }.take(MAXIMUM_CAUSES).any { it is ProtectedRouteMissingException }

private const val MAXIMUM_CAUSES = 8

/**
 * Whether something may leave the device without a circuit through at least [CircuitHopCount.PROTECTING] hops: only
 * when the user allows sending directly ([CircuitFallbackStore]). A circuit through one hop does not protect: its
 * single node sees both the device's address and where the request goes.
 */
class DirectSendingPolicy(
    private val circuits: CircuitRunner,
    private val fallback: CircuitFallbackStore
) {
    /** True while nothing may go out unprotected and no protecting circuit is up, so messages wait. */
    fun observeWaiting(): Flow<Boolean> =
        combine(circuits.status, fallback.observeSendsDirectly()) { status, sendsDirectly -> !sendsDirectly && !status.protects() }

    suspend fun sendsDirectly(): Boolean = fallback.sendsDirectly()

    /** For a send that cannot use the circuit: throws [ProtectedRouteMissingException] while it may not go out. */
    suspend fun checkUnprotectedSendAllowed() {
        if (!fallback.sendsDirectly() && circuits.currentCircuit(minimumHopCount = CircuitHopCount.PROTECTING) == null) {
            throw ProtectedRouteMissingException()
        }
    }

    private fun CircuitStatus.protects(): Boolean = this is CircuitStatus.Connected && hopCount >= CircuitHopCount.PROTECTING
}
