package com.cbgm.sparrow.feature.transport.di

import org.koin.dsl.module

val transportModule =
    module {
        includes(transportDataModule, directoryModule, controlPlaneModule, routeModule, mailboxModule, connectionModule, transportUseCaseModule)
    }

internal const val GATEWAY_JSON_QUALIFIER = "GatewayJson"
internal const val CONTROL_PLANE_HTTP_CLIENT_QUALIFIER = "ControlPlaneHttpClient"
