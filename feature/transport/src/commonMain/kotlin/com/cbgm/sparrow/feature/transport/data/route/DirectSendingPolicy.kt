package com.cbgm.sparrow.feature.transport.data.route

import com.cbgm.sparrow.feature.transport.data.preferences.TransportPreferencesLocalDataSource
import com.cbgm.sparrow.feature.transport.domain.model.CircuitHopCount
import com.cbgm.sparrow.feature.transport.domain.model.CircuitStatus
import com.cbgm.sparrow.feature.transport.domain.model.ProtectedRouteMissingException
import com.cbgm.sparrow.feature.transport.domain.port.CircuitPort
import com.cbgm.sparrow.feature.transport.domain.port.DirectSendingPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

internal class DirectSendingPolicy(
    private val circuits: CircuitPort,
    private val preferences: TransportPreferencesLocalDataSource
) : DirectSendingPort {
    override fun observeWaiting(): Flow<Boolean> =
        combine(circuits.status, preferences.observeSendsDirectly()) { status, sendsDirectly -> !sendsDirectly && !status.protects() }

    override suspend fun sendsDirectly(): Boolean = preferences.observeSendsDirectly().first()

    override suspend fun checkUnprotectedSendAllowed() {
        if (!preferences.observeSendsDirectly().first() && !circuits.hasCircuit(minimumHopCount = CircuitHopCount.PROTECTING)) {
            throw ProtectedRouteMissingException()
        }
    }

    override fun hasProtectingCircuit(): Boolean = circuits.hasCircuit(minimumHopCount = CircuitHopCount.PROTECTING)

    private fun CircuitStatus.protects(): Boolean = this is CircuitStatus.Connected && hopCount >= CircuitHopCount.PROTECTING
}
