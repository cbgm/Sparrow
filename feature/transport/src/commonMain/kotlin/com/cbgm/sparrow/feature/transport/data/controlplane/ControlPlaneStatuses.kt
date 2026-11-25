package com.cbgm.sparrow.feature.transport.data.controlplane

import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpoint
import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpointStatus
import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneReachability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** The configured Control Planes, how each answered last, and the one in use; in memory only. */
internal class ControlPlaneStatuses {
    private val _endpoints = MutableStateFlow(emptyList<ControlPlaneEndpoint>())
    private val _activeEndpoint = MutableStateFlow<ControlPlaneEndpoint?>(null)
    private val _statuses = MutableStateFlow(emptyList<ControlPlaneEndpointStatus>())

    val endpoints: StateFlow<List<ControlPlaneEndpoint>> = _endpoints.asStateFlow()
    val activeEndpoint: StateFlow<ControlPlaneEndpoint?> = _activeEndpoint.asStateFlow()
    val statuses: StateFlow<List<ControlPlaneEndpointStatus>> = _statuses.asStateFlow()

    /**
     * The endpoints are now [updated]: each keeps how it answered, and the active one stays unless it is [isStale]
     * while a fresh one exists.
     */
    fun replace(
        updated: List<ControlPlaneEndpoint>,
        isStale: (ControlPlaneEndpoint) -> Boolean
    ) {
        val previous = _statuses.value.associateBy { status -> status.endpoint.baseUrl }
        val previousActive = _activeEndpoint.value?.takeIf(updated::contains)
        val active =
            previousActive?.takeUnless { endpoint ->
                isStale(endpoint) && updated.any { !isStale(it) }
            } ?: updated.firstOrNull { !isStale(it) } ?: updated.firstOrNull()
        _endpoints.value = updated
        _activeEndpoint.value = active
        _statuses.value =
            updated.map { endpoint ->
                ControlPlaneEndpointStatus(
                    endpoint = endpoint,
                    reachability = previous[endpoint.baseUrl]?.reachability ?: ControlPlaneReachability.UNKNOWN,
                    isActive = endpoint == active
                )
            }
    }

    /**
     * The order to try them in: the active one, then the reachable ones, then the unreachable ones. A stale directory
     * entry remains available for the grace period, but it must never stay ahead of a freshly signed replacement.
     */
    fun ordered(isStale: (ControlPlaneEndpoint) -> Boolean): List<ControlPlaneEndpoint> {
        val configured = _endpoints.value
        val statusByEndpoint = _statuses.value.associateBy(ControlPlaneEndpointStatus::endpoint)
        val activeEndpoint = _activeEndpoint.value
        val fresh = configured.filterNot(isStale)
        val stale = configured.filter(isStale)
        return fresh.order(statusByEndpoint, activeEndpoint) + stale.order(statusByEndpoint, activeEndpoint)
    }

    fun markActive(endpoint: ControlPlaneEndpoint) {
        if (endpoint !in _endpoints.value) return
        _activeEndpoint.value = endpoint
        markAvailable(endpoint)
        _statuses.update { current ->
            current.map { status -> status.copy(isActive = status.endpoint == endpoint) }
        }
    }

    fun markAvailable(endpoint: ControlPlaneEndpoint) {
        updateReachability(endpoint, ControlPlaneReachability.AVAILABLE)
    }

    fun markUnreachable(endpoint: ControlPlaneEndpoint) {
        updateReachability(endpoint, ControlPlaneReachability.UNREACHABLE)
    }

    private fun List<ControlPlaneEndpoint>.order(
        statusByEndpoint: Map<ControlPlaneEndpoint, ControlPlaneEndpointStatus>,
        activeEndpoint: ControlPlaneEndpoint?
    ): List<ControlPlaneEndpoint> {
        val reachable =
            filter { endpoint ->
                statusByEndpoint[endpoint]?.reachability != ControlPlaneReachability.UNREACHABLE
            }
        val unreachable = filterNot(reachable::contains)
        val active = activeEndpoint?.takeIf(reachable::contains)
        return listOfNotNull(active) + reachable.filterNot { it == active } + unreachable
    }

    private fun updateReachability(
        endpoint: ControlPlaneEndpoint,
        reachability: ControlPlaneReachability
    ) {
        _statuses.update { current ->
            current.map { status ->
                if (status.endpoint == endpoint) status.copy(reachability = reachability) else status
            }
        }
    }
}
