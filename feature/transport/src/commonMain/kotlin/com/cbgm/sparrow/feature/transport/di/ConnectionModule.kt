package com.cbgm.sparrow.feature.transport.di

import com.cbgm.sparrow.feature.transport.data.TransportConfig
import com.cbgm.sparrow.feature.transport.data.connection.AliasClaimSchedule
import com.cbgm.sparrow.feature.transport.data.connection.HomeNodeConnection
import com.cbgm.sparrow.feature.transport.data.connection.NodeDirectoryRefresh
import com.cbgm.sparrow.feature.transport.data.connection.TransportDiagnosticsRepositoryImpl
import com.cbgm.sparrow.feature.transport.data.controlplane.AliasClaimRegistrar
import com.cbgm.sparrow.feature.transport.data.controlplane.ControlPlaneLocalDataSource
import com.cbgm.sparrow.feature.transport.data.controlplane.NodeControlPlaneDiscoverySynchronizer
import com.cbgm.sparrow.feature.transport.data.directory.NodeEndpointRemoteDataSource
import com.cbgm.sparrow.feature.transport.data.route.circuit.CircuitRunner
import com.cbgm.sparrow.feature.transport.data.routing.RoutingIdLocalDataSource
import com.cbgm.sparrow.feature.transport.domain.port.TransportConnectionPort
import com.cbgm.sparrow.feature.transport.domain.repository.TransportDiagnosticsRepository
import org.koin.dsl.module

/** The home node connection and its diagnostics. */
internal val connectionModule =
    module {
        single {
            TransportDiagnosticsRepositoryImpl(
                routingIdDataSource = get<RoutingIdLocalDataSource>(),
                nodeEndpointDataSource = get<NodeEndpointRemoteDataSource>(),
                controlPlaneConfiguration = get<ControlPlaneLocalDataSource>(),
                transportConfig = get<TransportConfig>()
            )
        }

        single {
            HomeNodeConnection(
                circuits = get<CircuitRunner>(),
                aliasClaims = AliasClaimSchedule(aliasClaims = get<AliasClaimRegistrar>(), routingIdDataSource = get<RoutingIdLocalDataSource>()),
                routingIdDataSource = get<RoutingIdLocalDataSource>(),
                nodeEndpointDataSource = get<NodeEndpointRemoteDataSource>(),
                directoryRefresh =
                    NodeDirectoryRefresh(
                        nodeEndpointDataSource = get<NodeEndpointRemoteDataSource>(),
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
