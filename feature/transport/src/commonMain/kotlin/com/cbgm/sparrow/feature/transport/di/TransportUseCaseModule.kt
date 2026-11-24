package com.cbgm.sparrow.feature.transport.di

import com.cbgm.sparrow.feature.transport.domain.usecase.AddManualControlPlaneUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.CanConnectWhileClosedUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ClearControlPlaneListCacheUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ClearControlPlaneListUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ForgetControlPlaneDirectoryTrustUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.GetCurrentNodeBaseUrlUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.GetGatewayNodesUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ImportControlPlaneListUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveCircuitCountUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveCircuitFallbackUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveCircuitHopCountUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveCircuitStatusUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveConnectionModeUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveDecoyCountUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveLocalNetworkNeededUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObservePreferredNodeUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveRouteProtectionUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveTransportDiagnosticsUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveWaitingForRouteUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.PrepareControlPlaneUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.RefreshControlPlaneDirectoryUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.RefreshControlPlaneHealthUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.RefreshTransportDiagnosticsUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.RemoveManualControlPlaneUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.RunCircuitsUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetCircuitCountUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetCircuitFallbackUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetCircuitHopCountUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetConnectionModeUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetControlPlaneDirectoryUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetDecoyCountUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetPreferredNodeUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SynchronizeControlPlaneDirectoryUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/** What other modules use. */
internal val transportUseCaseModule =
    module {
        singleOf(::GetGatewayNodesUseCase)

        singleOf(::ObservePreferredNodeUseCase)

        singleOf(::SetPreferredNodeUseCase)

        singleOf(::ObserveCircuitHopCountUseCase)

        singleOf(::SetCircuitHopCountUseCase)

        singleOf(::ObserveCircuitCountUseCase)

        singleOf(::SetCircuitCountUseCase)

        singleOf(::ObserveDecoyCountUseCase)

        singleOf(::ObserveLocalNetworkNeededUseCase)

        singleOf(::SetDecoyCountUseCase)

        singleOf(::ObserveCircuitStatusUseCase)

        singleOf(::ObserveCircuitFallbackUseCase)

        singleOf(::SetCircuitFallbackUseCase)

        singleOf(::ObserveWaitingForRouteUseCase)

        singleOf(::ObserveRouteProtectionUseCase)

        singleOf(::ObserveConnectionModeUseCase)

        singleOf(::SetConnectionModeUseCase)

        singleOf(::CanConnectWhileClosedUseCase)

        singleOf(::PrepareControlPlaneUseCase)

        singleOf(::RunCircuitsUseCase)
        singleOf(::ObserveTransportDiagnosticsUseCase)
        singleOf(::RefreshTransportDiagnosticsUseCase)
        singleOf(::GetCurrentNodeBaseUrlUseCase)
        singleOf(::RefreshControlPlaneHealthUseCase)
        singleOf(::AddManualControlPlaneUseCase)
        singleOf(::RemoveManualControlPlaneUseCase)
        singleOf(::SetControlPlaneDirectoryUseCase)
        singleOf(::ClearControlPlaneListUseCase)
        singleOf(::ClearControlPlaneListCacheUseCase)
        singleOf(::RefreshControlPlaneDirectoryUseCase)
        singleOf(::SynchronizeControlPlaneDirectoryUseCase)
        singleOf(::ImportControlPlaneListUseCase)
        singleOf(::ForgetControlPlaneDirectoryTrustUseCase)
    }
