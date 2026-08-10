package com.cbgm.securechat.server.gateway

import com.cbgm.securechat.server.protocol.EnvelopeAcceptanceState
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import com.cbgm.securechat.server.protocol.FederatedTypingEvent
import com.cbgm.securechat.server.protocol.FederationAcknowledgement
import com.cbgm.securechat.server.protocol.GatewayLoad
import com.cbgm.securechat.server.protocol.GatewayNodeInformation
import com.cbgm.securechat.server.security.InternalApiAuthentication
import com.cbgm.securechat.server.security.NodeIdentity
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import kotlinx.serialization.Serializable

internal fun Application.installGatewayRoutes(
    runtime: GatewayRuntime,
    identity: NodeIdentity,
    config: GatewayConfig
) {
    routing {
        installHealthRoute(runtime)
        installInformationRoute(identity, config)
        installControlPlaneDiscoveryRoute(config)
        installRelayRoute(runtime)
        installIncomingEnvelopeRoute(runtime, config)
        installIncomingTypingRoute(runtime, config)
        installInternalLoadRoute(runtime, config)
        installInternalRouteResolution(runtime, config)
    }
}

private fun Route.installHealthRoute(runtime: GatewayRuntime) {
    get("/health") {
        call.respondText("ok connections=${runtime.connections.count()}")
    }
}

private fun Route.installInformationRoute(
    identity: NodeIdentity,
    config: GatewayConfig
) {
    get("/v1/gateway") {
        call.respond(
            GatewayNodeInformation(
                nodeId = identity.nodeId,
                routeLifetimeMilliseconds = config.routeLifetimeMilliseconds,
                routeRefreshIntervalMilliseconds = config.routeRefreshIntervalMilliseconds
            )
        )
    }
}

private fun Route.installControlPlaneDiscoveryRoute(config: GatewayConfig) {
    get("/v1/control-planes") {
        call.respond(
            GatewayControlPlaneDirectory(
                controlPlanes = config.advertisedControlPlaneUrls
            )
        )
    }
}

private fun Route.installRelayRoute(runtime: GatewayRuntime) {
    webSocket("/relay") {
        runtime.handler.handle(this)
    }
}

private fun Route.installIncomingEnvelopeRoute(
    runtime: GatewayRuntime,
    config: GatewayConfig
) {
    post("/internal/v1/envelopes") {
        if (call.hasInternalAccess(config.gatewayInternalApiToken)) {
            val envelope = call.receive<FederatedEnvelope>()
            val accepted = runtime.handler.acceptIncoming(envelope)
            call.respondToIncomingEnvelope(envelope, accepted)
        } else {
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
}

private fun Route.installIncomingTypingRoute(
    runtime: GatewayRuntime,
    config: GatewayConfig
) {
    post("/internal/v1/typing-events") {
        if (call.hasInternalAccess(config.gatewayInternalApiToken)) {
            val delivered =
                runtime.handler.acceptIncomingTyping(
                    call.receive<FederatedTypingEvent>()
                )
            call.respond(
                if (delivered) {
                    HttpStatusCode.Accepted
                } else {
                    HttpStatusCode.NotFound
                }
            )
        } else {
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
}

private fun Route.installInternalLoadRoute(
    runtime: GatewayRuntime,
    config: GatewayConfig
) {
    get("/internal/v1/load") {
        if (!call.hasInternalAccess(config.gatewayInternalApiToken)) {
            call.respond(HttpStatusCode.Unauthorized)
            return@get
        }

        call.respond(
            GatewayLoad(
                activeConnections = runtime.connections.count()
            )
        )
    }
}

private fun Route.installInternalRouteResolution(
    runtime: GatewayRuntime,
    config: GatewayConfig
) {
    get("/internal/v1/routes/{routingId}") {
        if (!call.hasInternalAccess(config.gatewayInternalApiToken)) {
            call.respond(HttpStatusCode.Unauthorized)
            return@get
        }

        val routingId = call.parameters["routingId"]
        val canonicalRoutingId =
            routingId?.let(runtime.connections::resolveCanonicalRoutingId)

        if (canonicalRoutingId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respondText(canonicalRoutingId)
        }
    }
}

private suspend fun ApplicationCall.respondToIncomingEnvelope(
    envelope: FederatedEnvelope,
    accepted: Boolean
) {
    if (accepted) {
        respond(
            status = HttpStatusCode.Accepted,
            message =
                FederationAcknowledgement(
                    envelopeId = envelope.envelopeId,
                    state = EnvelopeAcceptanceState.STORED_AT_DESTINATION
                )
        )
    } else {
        respond(HttpStatusCode.ServiceUnavailable)
    }
}

private fun ApplicationCall.hasInternalAccess(expectedToken: String?): Boolean =
    InternalApiAuthentication.isAuthorized(
        expectedToken,
        request.headers[InternalApiAuthentication.TOKEN_HEADER]
    )

@Serializable
private data class GatewayControlPlaneDirectory(
    val controlPlanes: List<String>
)
