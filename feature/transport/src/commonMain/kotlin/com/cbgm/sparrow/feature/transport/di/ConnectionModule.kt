package com.cbgm.sparrow.feature.transport.di

import com.cbgm.sparrow.feature.transport.data.TransportConfig
import com.cbgm.sparrow.feature.transport.data.connection.AliasClaimSchedule
import com.cbgm.sparrow.feature.transport.data.connection.HomeNodeConnection
import com.cbgm.sparrow.feature.transport.data.connection.HomeNodeDiagnostics
import com.cbgm.sparrow.feature.transport.data.connection.NodeDirectoryRefresh
import com.cbgm.sparrow.feature.transport.data.controlplane.AliasClaimRegistrar
import com.cbgm.sparrow.feature.transport.data.controlplane.ControlPlaneEndpointStore
import com.cbgm.sparrow.feature.transport.data.controlplane.NodeControlPlaneDiscoverySynchronizer
import com.cbgm.sparrow.feature.transport.data.route.circuit.CircuitRunner
import com.cbgm.sparrow.feature.transport.domain.gateway.LocalRoutingIdSource
import com.cbgm.sparrow.feature.transport.domain.gateway.NodeEndpointResolver
import com.cbgm.sparrow.feature.transport.domain.gateway.TransportConnection
import com.cbgm.sparrow.feature.transport.domain.gateway.TransportDiagnosticsSource
import org.koin.dsl.module

/** The home node connection and its diagnostics. */
internal val connectionModule =
    module {
        single {
            HomeNodeDiagnostics(
                localRoutingIdSource = get<LocalRoutingIdSource>(),
                nodeEndpointResolver = get<NodeEndpointResolver>(),
                controlPlaneConfiguration = get<ControlPlaneEndpointStore>(),
                transportConfig = get<TransportConfig>()
            )
        }

        single {
            HomeNodeConnection(
                circuits = get<CircuitRunner>(),
                aliasClaims = AliasClaimSchedule(aliasClaims = get<AliasClaimRegistrar>(), localRoutingIdSource = get<LocalRoutingIdSource>()),
                localRoutingIdSource = get<LocalRoutingIdSource>(),
                nodeEndpointResolver = get<NodeEndpointResolver>(),
                directoryRefresh =
                    NodeDirectoryRefresh(
                        nodeEndpointResolver = get<NodeEndpointResolver>(),
                        controlPlaneDiscoverySynchronizer = get<NodeControlPlaneDiscoverySynchronizer>(),
                        transportConfig = get<TransportConfig>(),
                        diagnostics = get<HomeNodeDiagnostics>()
                    ),
                diagnostics = get<HomeNodeDiagnostics>(),
                preferences = get()
            )
        }

        single<TransportConnection> {
            get<HomeNodeConnection>()
        }

        single<TransportDiagnosticsSource> {
            get<HomeNodeDiagnostics>()
        }
    }
