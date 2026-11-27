package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.domain.port.TransportConnectionPort
import com.cbgm.sparrow.feature.transport.domain.repository.TransportPreferencesRepository

/**
 * Saves the favourite gateway (null for automatic) and moves the connection to it right away. If it
 * cannot be reached, the connection fails over to the other gateways as usual.
 */
class SetPreferredNodeUseCase(
    private val preferences: TransportPreferencesRepository,
    private val transportConnection: TransportConnectionPort
) {
    suspend operator fun invoke(nodeId: String?): Result<Unit> =
        safeSuspendCall {
            preferences.setPreferredNode(nodeId)
            nodeId?.let { preferred -> transportConnection.moveTo(preferred) }
        }
}
