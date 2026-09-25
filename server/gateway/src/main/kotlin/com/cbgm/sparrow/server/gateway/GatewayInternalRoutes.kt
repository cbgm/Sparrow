package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.EnvelopeAcceptanceState
import com.cbgm.sparrow.server.protocol.FederatedEnvelope
import com.cbgm.sparrow.server.protocol.FederatedIndicatorEvent
import com.cbgm.sparrow.server.protocol.FederationAcknowledgement
import com.cbgm.sparrow.server.protocol.GatewayLoad
import com.cbgm.sparrow.server.security.InternalApiAuthentication
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

internal fun Route.installGatewayInternalRoutes(runtime: GatewayRuntime, config: GatewayConfig) {
    installIncomingEnvelopeRoute(runtime, config)
    installIncomingIndicatorRoute(runtime, config)
    installInternalLoadRoute(runtime, config)
    installInternalRouteResolution(runtime, config)
}

private fun Route.installIncomingEnvelopeRoute(runtime: GatewayRuntime, config: GatewayConfig) {
    post("/internal/v1/envelopes") {
        if (!call.hasInternalAccess(config.gatewayInternalApiToken)) {
            call.respond(HttpStatusCode.Unauthorized)
            return@post
        }
        val envelope = call.receive<FederatedEnvelope>()
        call.respondToIncomingEnvelope(envelope, runtime.handler.acceptIncoming(envelope))
    }
}

private fun Route.installIncomingIndicatorRoute(runtime: GatewayRuntime, config: GatewayConfig) {
    post("/internal/v1/indicator-events") {
        if (!call.hasInternalAccess(config.gatewayInternalApiToken)) {
            call.respond(HttpStatusCode.Unauthorized)
            return@post
        }
        val delivered = runtime.handler.acceptIncomingIndicator(call.receive<FederatedIndicatorEvent>())
        call.respond(if (delivered) HttpStatusCode.Accepted else HttpStatusCode.NotFound)
    }
}

private fun Route.installInternalLoadRoute(runtime: GatewayRuntime, config: GatewayConfig) {
    get("/internal/v1/load") {
        if (!call.hasInternalAccess(config.gatewayInternalApiToken)) {
            call.respond(HttpStatusCode.Unauthorized)
            return@get
        }
        call.respond(GatewayLoad(activeConnections = runtime.connections.count()))
    }
}

private fun Route.installInternalRouteResolution(runtime: GatewayRuntime, config: GatewayConfig) {
    get("/internal/v1/routes/{routingId}") {
        if (!call.hasInternalAccess(config.gatewayInternalApiToken)) {
            call.respond(HttpStatusCode.Unauthorized)
            return@get
        }
        val canonicalRoutingId = call.parameters["routingId"]
            ?.let(runtime.connections::resolveCanonicalRoutingId)
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
            message = FederationAcknowledgement(
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
