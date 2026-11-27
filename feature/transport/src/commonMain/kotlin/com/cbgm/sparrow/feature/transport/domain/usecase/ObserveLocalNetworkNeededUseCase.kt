package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneConfigurationRepository
import com.cbgm.sparrow.feature.transport.domain.repository.TransportDiagnosticsRepository
import com.cbgm.sparrow.feature.transport.domain.rule.LocalNetworkAddresses
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Whether Sparrow talks to a server on the local network: the Control Plane directory, a Control Plane or a node from
 * the node directory has a local address ([LocalNetworkAddresses]). Android 17 then needs the local network permission.
 */
class ObserveLocalNetworkNeededUseCase(
    private val configuration: ControlPlaneConfigurationRepository,
    private val diagnostics: TransportDiagnosticsRepository
) {
    operator fun invoke(): Flow<Boolean> =
        combine(configuration.directoryUrl, configuration.endpoints, diagnostics.diagnostics) { directoryUrl, endpoints, state ->
            (listOfNotNull(directoryUrl) + endpoints.map { endpoint -> endpoint.baseUrl } + state.availableNodes.map { node -> node.websocketUrl })
                .any(LocalNetworkAddresses::isLocal)
        }.distinctUntilChanged()
}
