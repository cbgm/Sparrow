package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionManager
import com.cbgm.sparrow.feature.transport.discovery.PreferredNodeStore

/**
 * Saves the favourite gateway (null for automatic) and moves the connection to it right away. If it
 * cannot be reached, the connection fails over to the other gateways as usual.
 */
class SetPreferredNodeUseCase(
    private val preferredNodeStore: PreferredNodeStore,
    private val transportConnectionManager: TransportConnectionManager
) {
    suspend operator fun invoke(nodeId: String?): Result<Unit> =
        safeSuspendCall {
            preferredNodeStore.set(nodeId)
            nodeId?.let { preferred -> transportConnectionManager.moveTo(preferred) }
        }
}
