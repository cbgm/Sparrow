package com.cbgm.sparrow.server.push

import com.cbgm.sparrow.server.protocol.PendingTransportEnvelopesResponse
import com.cbgm.sparrow.server.protocol.TransportEnvelope
import com.cbgm.sparrow.server.security.InternalApiAuthentication
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

internal fun Application.installPushRoutes(
    runtime: PushRuntime,
    config: PushConfig
) {
    routing {
        installHealthRoute(runtime)
        installNodePushRoutes(runtime)
        installInternalEnvelopeRoute(runtime, config)
        installInternalPendingRoute(runtime, config)
        installInternalAcknowledgementRoute(runtime, config)
    }
}

private fun Route.installHealthRoute(runtime: PushRuntime) {
    get("/health") {
        call.respondText(
            "ok persistence=${runtime.stores.persistenceMode} " +
                "pendingEnvelopes=${runtime.pendingEnvelopes.count()}"
        )
    }
}

private fun Route.installInternalEnvelopeRoute(
    runtime: PushRuntime,
    config: PushConfig
) {
    post("/internal/v1/envelopes") {
        if (call.hasInternalAccess(config.pushInternalApiToken)) {
            val accepted = runtime.pendingEnvelopes.store(call.receive<TransportEnvelope>())
            call.respond(
                if (accepted) {
                    HttpStatusCode.Accepted
                } else {
                    HttpStatusCode.InsufficientStorage
                }
            )
        } else {
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
}

private fun Route.installInternalPendingRoute(
    runtime: PushRuntime,
    config: PushConfig
) {
    get("/internal/v1/recipients/{recipientId}/envelopes") {
        if (call.hasInternalAccess(config.pushInternalApiToken)) {
            val recipientId = call.parameters["recipientId"]
            if (recipientId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest)
            } else {
                call.respond(
                    PendingTransportEnvelopesResponse(
                        runtime.pendingEnvelopes.pending(recipientId)
                    )
                )
            }
        } else {
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
}

private fun Route.installInternalAcknowledgementRoute(
    runtime: PushRuntime,
    config: PushConfig
) {
    post("/internal/v1/recipients/{recipientId}/envelopes/{envelopeId}/ack") {
        if (call.hasInternalAccess(config.pushInternalApiToken)) {
            val recipientId = call.parameters["recipientId"]
            val envelopeId = call.parameters["envelopeId"]
            if (recipientId.isNullOrBlank() || envelopeId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest)
            } else {
                runtime.pendingEnvelopes.remove(recipientId, envelopeId)
                call.respond(HttpStatusCode.NoContent)
            }
        } else {
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
}

private fun ApplicationCall.hasInternalAccess(expectedToken: String?): Boolean =
    InternalApiAuthentication.isAuthorized(
        expectedToken,
        request.headers[InternalApiAuthentication.TOKEN_HEADER]
    )
