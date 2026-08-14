package com.cbgm.securechat.server.push

import com.cbgm.securechat.server.protocol.PendingTransportEnvelopesResponse
import com.cbgm.securechat.server.protocol.PushDeviceRegistrationRequest
import com.cbgm.securechat.server.protocol.TransportEnvelope
import com.cbgm.securechat.server.security.InternalApiAuthentication
import com.cbgm.securechat.server.security.enforceRateLimit
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
        installDeviceRegistrationRoute(runtime)
        installWakeUpInboxRoute(runtime)
        installWakeUpAcknowledgementRoute(runtime)
        installNodePushRoutes(runtime)
        installInternalEnvelopeRoute(runtime, config)
        installInternalWakeUpRoute(runtime, config)
        installInternalPendingRoute(runtime, config)
        installInternalAcknowledgementRoute(runtime, config)
    }
}

private fun Route.installHealthRoute(runtime: PushRuntime) {
    get("/health") {
        call.respondText(
            "ok fcmEnabled=${runtime.fcmEnabled} " +
                "persistence=${runtime.stores.persistenceMode} " +
                "devices=${runtime.devices.count()} " +
                "pendingEnvelopes=${runtime.pendingEnvelopes.count()}"
        )
    }
}

private fun Route.installDeviceRegistrationRoute(runtime: PushRuntime) {
    post("/push/devices") {
        if (call.enforceRateLimit(runtime.deviceRegistrationRateLimiter)) {
            val request = call.receive<PushDeviceRegistrationRequest>()
            if (request.isValid()) {
                runtime.devices.register(
                    PushDevice(
                        routingId = request.routingId,
                        token = request.token,
                        platform = request.platform
                    )
                )
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}

private fun Route.installWakeUpInboxRoute(runtime: PushRuntime) {
    get("/push/wake/{wakeUpId}/inbox") {
        val recipientId = runtime.wakeUps.resolve(call.parameters["wakeUpId"])
        if (recipientId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(
                PendingTransportEnvelopesResponse(
                    runtime.pendingEnvelopes.pending(recipientId)
                )
            )
        }
    }
}

private fun Route.installWakeUpAcknowledgementRoute(runtime: PushRuntime) {
    post("/push/wake/{wakeUpId}/inbox/{envelopeId}/ack") {
        val recipientId = runtime.wakeUps.resolve(call.parameters["wakeUpId"])
        val envelopeId = call.parameters["envelopeId"]
        if (recipientId == null || envelopeId.isNullOrBlank()) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            runtime.pendingEnvelopes.remove(recipientId, envelopeId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun Route.installInternalEnvelopeRoute(
    runtime: PushRuntime,
    config: PushConfig
) {
    post("/internal/v1/envelopes") {
        if (call.hasInternalAccess(config.pushInternalApiToken)) {
            val accepted = runtime.coordinator.accept(call.receive<TransportEnvelope>())
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

private fun Route.installInternalWakeUpRoute(
    runtime: PushRuntime,
    config: PushConfig
) {
    post("/internal/v1/wake-ups/{recipientId}") {
        if (call.hasInternalAccess(config.pushInternalApiToken)) {
            val recipientId = call.parameters["recipientId"]
            if (recipientId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest)
            } else {
                runtime.coordinator.notifyRecipient(recipientId)
                call.respond(HttpStatusCode.Accepted)
            }
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

private fun PushDeviceRegistrationRequest.isValid(): Boolean =
    listOf(routingId, token, platform).all(String::isNotBlank)

private fun ApplicationCall.hasInternalAccess(expectedToken: String?): Boolean =
    InternalApiAuthentication.isAuthorized(
        expectedToken,
        request.headers[InternalApiAuthentication.TOKEN_HEADER]
    )
