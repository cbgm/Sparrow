package com.cbgm.sparrow.feature.transport.domain.rule

import com.cbgm.sparrow.feature.transport.domain.model.CircuitHopCount
import com.cbgm.sparrow.feature.transport.domain.model.CircuitStatus
import com.cbgm.sparrow.feature.transport.domain.model.ProtectedRouteMissingException
import com.cbgm.sparrow.feature.transport.domain.port.CircuitPort
import com.cbgm.sparrow.feature.transport.domain.repository.TransportPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Whether something may leave the device without a circuit through at least [CircuitHopCount.PROTECTING] hops: only
 * when the user allows sending directly ([TransportPreferencesRepository.observeSendsDirectly]). A circuit through one hop does not protect: its
 * single node sees both the device's address and where the request goes.
 */
class DirectSendingPolicy(
    private val circuits: CircuitPort,
    private val preferences: TransportPreferencesRepository
) {
    /** True while nothing may go out unprotected and no protecting circuit is up, so messages wait. */
    fun observeWaiting(): Flow<Boolean> =
        combine(circuits.status, preferences.observeSendsDirectly()) { status, sendsDirectly -> !sendsDirectly && !status.protects() }

    suspend fun sendsDirectly(): Boolean = preferences.observeSendsDirectly().first()

    /** For a send that cannot use the circuit: throws [ProtectedRouteMissingException] while it may not go out. */
    suspend fun checkUnprotectedSendAllowed() {
        if (!preferences.observeSendsDirectly().first() && !circuits.hasCircuit(minimumHopCount = CircuitHopCount.PROTECTING)) {
            throw ProtectedRouteMissingException()
        }
    }

    /** Whether a circuit through at least [CircuitHopCount.PROTECTING] hops is up, so a send hides the device's address. */
    fun hasProtectingCircuit(): Boolean = circuits.hasCircuit(minimumHopCount = CircuitHopCount.PROTECTING)

    private fun CircuitStatus.protects(): Boolean = this is CircuitStatus.Connected && hopCount >= CircuitHopCount.PROTECTING
}
