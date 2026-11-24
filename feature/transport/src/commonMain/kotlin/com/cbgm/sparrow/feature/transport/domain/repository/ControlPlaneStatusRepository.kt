package com.cbgm.sparrow.feature.transport.domain.repository

import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpoint
import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpointStatus
import kotlinx.coroutines.flow.StateFlow

interface ControlPlaneStatusRepository {
    val statuses: StateFlow<List<ControlPlaneEndpointStatus>>

    fun markAvailable(endpoint: ControlPlaneEndpoint)

    fun markUnreachable(endpoint: ControlPlaneEndpoint)
}
