package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.feature.transport.ControlPlaneReachability
import com.cbgm.sparrow.presentation.model.AppInitializationDependencies
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class ObserveControlPlaneRegistrationTargetsStartupTask(
    initialization: AppInitializationDependencies
) : IdentityReadyStartupTask(initialization) {
    override val name = "control-plane registration-target observer"

    override suspend fun runAfterIdentityReady() {
        combine(
            initialization.controlPlaneConfiguration.endpoints,
            initialization.controlPlaneStatusStore.statuses,
            initialization.controlPlaneConfiguration.activeEndpoint
        ) { endpoints, statuses, activeEndpoint ->
            val reachabilityByUrl =
                statuses.associate { status ->
                    status.endpoint.baseUrl to status.reachability
                }

            endpoints
                .filter { endpoint ->
                    reachabilityByUrl[endpoint.baseUrl] !=
                        ControlPlaneReachability.UNREACHABLE
                }.map { endpoint -> endpoint.baseUrl }
                .toSet() to activeEndpoint?.baseUrl
        }.distinctUntilChanged()
            .collectLatest {
                initialization.platformNotificationRuntime
                    .requestPushTokenRegistration()
            }
    }
}
