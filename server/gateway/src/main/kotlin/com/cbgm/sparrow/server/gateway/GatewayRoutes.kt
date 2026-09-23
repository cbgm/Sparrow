package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.GatewayNodeInformation
import com.cbgm.sparrow.server.security.NodeIdentity
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket

internal fun Application.installGatewayRoutes(
    runtime: GatewayRuntime,
    identity: NodeIdentity,
    config: GatewayConfig
) {
    routing {
        installHealthRoute(runtime)
        installInformationRoute(identity, config)
        installControlPlaneDiscoveryRoute(config)
        installGatewayWebSocketRoute(runtime)
        installBlobRoutes(runtime.blobStore, runtime.blobUploadPermitStore)
        installGatewayInternalRoutes(runtime, config)
    }
}

private fun Route.installHealthRoute(runtime: GatewayRuntime) {
    get("/health") {
        call.respondText("ok connections=${runtime.connections.count()}")
    }
}

private fun Route.installInformationRoute(identity: NodeIdentity, config: GatewayConfig) {
    get("/v1/gateway/info") {
        call.response.headers.append(SERVER_TIME_HEADER, System.currentTimeMillis().toString())
        call.respond(
            GatewayNodeInformation(
                nodeId = identity.nodeId,
                routeLifetimeMilliseconds = config.routeLifetimeMilliseconds,
                routeRefreshIntervalMilliseconds = config.routeRefreshIntervalMilliseconds
            )
        )
    }
}

private fun Route.installGatewayWebSocketRoute(runtime: GatewayRuntime) {
    webSocket("/v1/gateway") {
        runtime.handler.handle(this)
    }
}

private const val SERVER_TIME_HEADER = "X-Sparrow-Server-Time"
