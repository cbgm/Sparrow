package com.cbgm.sparrow.feature.transport.domain.gateway

import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpoint
import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpointStatus
import kotlinx.coroutines.flow.StateFlow

interface ControlPlaneStatusStore {
    val statuses: StateFlow<List<ControlPlaneEndpointStatus>>

    fun markAvailable(endpoint: ControlPlaneEndpoint)

    fun markUnreachable(endpoint: ControlPlaneEndpoint)
}
