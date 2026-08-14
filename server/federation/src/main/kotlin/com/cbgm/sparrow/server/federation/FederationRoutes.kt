package com.cbgm.sparrow.server.federation

import com.cbgm.sparrow.server.protocol.EnvelopeAcceptanceState
import com.cbgm.sparrow.server.protocol.ErrorResponse
import com.cbgm.sparrow.server.protocol.FederatedEnvelope
import com.cbgm.sparrow.server.protocol.FederatedTypingEvent
import com.cbgm.sparrow.server.protocol.FederationAcknowledgement
import com.cbgm.sparrow.server.protocol.NodeCapability
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.InternalApiAuthentication
import com.cbgm.sparrow.server.security.NodeRequestAuthentication
import com.cbgm.sparrow.server.security.NodeRequestHeaders
import com.cbgm.sparrow.server.security.Signatures
import com.cbgm.sparrow.server.security.enforceRateLimit
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

private const val FEDERATION_ENVELOPES_PATH = "/v1/federation/envelopes"
private const val FEDERATION_TYPING_EVENTS_PATH = "/v1/federation/typing-events"

internal fun Application.installFederationRoutes(
    runtime: FederationRuntime,
    config: FederationConfig
) {
    routing {
        installHealthRoutes(runtime)
        installInternalRoutes(runtime.router, config)
        installIncomingFederationRoutes(runtime)
        installRouteProbe(runtime)
    }
}

private fun Routing.installHealthRoutes(runtime: FederationRuntime) {
    get("/health") {
        call.respondText(
            "ok persistence=${runtime.outboundQueue.persistenceMode} " +
                "pending=${runtime.router.pendingCount()}"
        )
    }
    get("/v1/federation/capabilities") {
        call.respond(setOf(NodeCapability.FEDERATION))
    }
}

private fun Routing.installInternalRoutes(
    router: FederationRouter,
    config: FederationConfig
) {
    post("/internal/v1/outgoing-envelopes") {
        if (!call.hasInternalAccess(config.federationInternalApiToken)) {
            call.respond(HttpStatusCode.Unauthorized)
            return@post
        }
        val acknowledgement = router.route(call.receive<FederatedEnvelope>())
        call.respond(HttpStatusCode.Accepted, acknowledgement)
    }

    post("/internal/v1/outgoing-typing-events") {
        if (!call.hasInternalAccess(config.federationInternalApiToken)) {
            call.respond(HttpStatusCode.Unauthorized)
            return@post
        }
        val delivered = router.routeTyping(call.receive<FederatedTypingEvent>())
        call.respond(if (delivered) HttpStatusCode.Accepted else HttpStatusCode.NotFound)
    }

    post("/internal/v1/outgoing-envelopes/{envelopeId}/stored") {
        if (!call.hasInternalAccess(config.federationInternalApiToken)) {
            call.respond(HttpStatusCode.Unauthorized)
            return@post
        }
        val envelopeId = call.parameters["envelopeId"]
        if (envelopeId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }
        router.markStored(envelopeId)
        call.respond(HttpStatusCode.NoContent)
    }
}

private fun Routing.installIncomingFederationRoutes(runtime: FederationRuntime) {
    post(FEDERATION_ENVELOPES_PATH) {
        if (!call.enforceRateLimit(runtime.incomingRateLimiter)) {
            return@post
        }
        val body = call.receiveText()
        val envelope =
            runCatching { serverJson.decodeFromString<FederatedEnvelope>(body) }
                .getOrElse {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("INVALID_ENVELOPE", "Invalid envelope")
                    )
                    return@post
                }
        if (!call.hasValidNodeAuthentication(FEDERATION_ENVELOPES_PATH, body, runtime)) {
            call.respondInvalidNodeAuthentication()
            return@post
        }
        if (runtime.incomingIds.contains(envelope.envelopeId)) {
            call.respondDuplicate(envelope.envelopeId)
            return@post
        }

        val acknowledgement = runtime.localGateway.deliver(envelope)
        if (acknowledgement.state == EnvelopeAcceptanceState.STORED_AT_DESTINATION) {
            runtime.incomingIds.record(
                envelope.envelopeId,
                envelope.expiresAtEpochMilliseconds
            )
        }
        call.respond(HttpStatusCode.Accepted, acknowledgement)
    }

    post(FEDERATION_TYPING_EVENTS_PATH) {
        if (!call.enforceRateLimit(runtime.incomingRateLimiter)) {
            return@post
        }
        val body = call.receiveText()
        val event =
            runCatching { serverJson.decodeFromString<FederatedTypingEvent>(body) }
                .getOrElse {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("INVALID_TYPING_EVENT", "Invalid typing event")
                    )
                    return@post
                }
        if (!call.hasValidNodeAuthentication(FEDERATION_TYPING_EVENTS_PATH, body, runtime)) {
            call.respondInvalidNodeAuthentication()
            return@post
        }

        val delivered = runtime.router.routeTyping(event)
        call.respond(if (delivered) HttpStatusCode.Accepted else HttpStatusCode.NotFound)
    }
}

private fun Routing.installRouteProbe(runtime: FederationRuntime) {
    get("/v1/federation/routes/{routingId}") {
        if (!call.enforceRateLimit(runtime.incomingRateLimiter)) {
            return@get
        }

        val routingId = call.parameters["routingId"]
        if (routingId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        val path = "/v1/federation/routes/$routingId"
        if (!call.hasValidNodeAuthentication(path, "", runtime)) {
            call.respondInvalidNodeAuthentication()
            return@get
        }

        val canonicalRoutingId =
            (runtime.localGateway as? LocalRouteResolver)?.resolve(routingId)
        if (canonicalRoutingId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respondText(canonicalRoutingId)
        }
    }
}

private suspend fun ApplicationCall.respondInvalidNodeAuthentication() {
    respond(
        HttpStatusCode.Unauthorized,
        ErrorResponse("INVALID_NODE_AUTH", "Node authentication failed")
    )
}

private suspend fun ApplicationCall.respondDuplicate(envelopeId: String) {
    respond(
        FederationAcknowledgement(
            envelopeId = envelopeId,
            state = EnvelopeAcceptanceState.STORED_AT_DESTINATION,
            duplicate = true
        )
    )
}

private fun ApplicationCall.hasInternalAccess(expectedToken: String?): Boolean =
    InternalApiAuthentication.isAuthorized(
        expectedToken,
        request.headers[InternalApiAuthentication.TOKEN_HEADER]
    )

private fun ApplicationCall.requestAuthentication(): NodeRequestAuthentication? {
    val nodeId = request.headers[NodeRequestHeaders.NODE_ID]
    val timestamp = request.headers[NodeRequestHeaders.TIMESTAMP]?.toLongOrNull()
    val nonce = request.headers[NodeRequestHeaders.NONCE]
    val signature = request.headers[NodeRequestHeaders.SIGNATURE]

    if (listOf(nodeId, timestamp, nonce, signature).any { it == null }) {
        return null
    }
    return NodeRequestAuthentication(
        nodeId = requireNotNull(nodeId),
        timestampEpochMilliseconds = requireNotNull(timestamp),
        nonce = requireNotNull(nonce),
        signature = requireNotNull(signature)
    )
}

private suspend fun ApplicationCall.hasValidNodeAuthentication(
    path: String,
    body: String,
    runtime: FederationRuntime
): Boolean {
    val authentication = requestAuthentication()
    val descriptor = authentication?.let { runtime.registry.find(it.nodeId) }

    return authentication != null &&
        descriptor != null &&
        runtime.verifier.verify(
            authentication = authentication,
            method = request.httpMethod.value,
            path = path,
            body = body,
            publicKey = Signatures.decodePublicKey(descriptor.identityPublicKey)
        )
}
