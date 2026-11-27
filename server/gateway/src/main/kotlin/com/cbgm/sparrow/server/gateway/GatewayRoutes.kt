package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.security.NodeIdentity
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

internal fun Application.installGatewayRoutes(
    runtime: GatewayRuntime,
    identity: NodeIdentity,
    config: GatewayConfig
) {
    routing {
        installHealthRoute()
        installControlPlaneDiscoveryRoute(config)
        installChunkedBlobRoutes(runtime.blobStore, identity.nodeId, runtime.blobCreationWork, config.maximumBlobRetentionMilliseconds)
        installRelayRoute(runtime.relay)
        installCircuitRoute(runtime.circuits)
    }
}

private fun Route.installHealthRoute() {
    get("/health") {
        call.respondText("ok")
    }
}
