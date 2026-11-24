package com.cbgm.sparrow.feature.transport.di

import com.cbgm.sparrow.feature.transport.data.TransportConfig
import com.cbgm.sparrow.feature.transport.data.connection.AliasClaimSchedule
import com.cbgm.sparrow.feature.transport.data.connection.HomeNodeConnection
import com.cbgm.sparrow.feature.transport.data.connection.NodeDirectoryRefresh
import com.cbgm.sparrow.feature.transport.data.connection.TransportDiagnosticsRepositoryImpl
import com.cbgm.sparrow.feature.transport.data.controlplane.AliasClaimRegistrar
import com.cbgm.sparrow.feature.transport.data.controlplane.ControlPlaneEndpointStore
import com.cbgm.sparrow.feature.transport.data.controlplane.NodeControlPlaneDiscoverySynchronizer
import com.cbgm.sparrow.feature.transport.data.route.circuit.CircuitRunner
import com.cbgm.sparrow.feature.transport.domain.port.TransportConnectionPort
import com.cbgm.sparrow.feature.transport.domain.repository.NodeEndpointRepository
import com.cbgm.sparrow.feature.transport.domain.repository.RoutingIdRepository
import com.cbgm.sparrow.feature.transport.domain.repository.TransportDiagnosticsRepository
import org.koin.dsl.module

/** The home node connection and its diagnostics. */
internal val connectionModule =
    module {
        single {
            TransportDiagnosticsRepositoryImpl(
                routingIdRepository = get<RoutingIdRepository>(),
                nodeEndpointResolver = get<NodeEndpointRepository>(),
                controlPlaneConfiguration = get<ControlPlaneEndpointStore>(),
                transportConfig = get<TransportConfig>()
            )
        }

        single {
            HomeNodeConnection(
                circuits = get<CircuitRunner>(),
                aliasClaims = AliasClaimSchedule(aliasClaims = get<AliasClaimRegistrar>(), routingIdRepository = get<RoutingIdRepository>()),
                routingIdRepository = get<RoutingIdRepository>(),
                nodeEndpointResolver = get<NodeEndpointRepository>(),
                directoryRefresh =
                    NodeDirectoryRefresh(
                        nodeEndpointResolver = get<NodeEndpointRepository>(),
                        controlPlaneDiscoverySynchronizer = get<NodeControlPlaneDiscoverySynchronizer>(),
                        transportConfig = get<TransportConfig>(),
                        diagnostics = get<TransportDiagnosticsRepositoryImpl>()
                    ),
                diagnostics = get<TransportDiagnosticsRepositoryImpl>(),
                preferences = get()
            )
        }

        single<TransportConnectionPort> {
            get<HomeNodeConnection>()
        }

        single<TransportDiagnosticsRepository> {
            get<TransportDiagnosticsRepositoryImpl>()
        }
    }
