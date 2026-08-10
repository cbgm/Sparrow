package com.cbgm.securechat.server.push

import com.cbgm.securechat.server.protocol.NodeCapability
import com.cbgm.securechat.server.protocol.PendingRelayEnvelopesResponse
import com.cbgm.securechat.server.protocol.RelayEnvelope
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.security.NodeRequestAuthorizationRequirements
import com.cbgm.securechat.server.security.nodeRequestAuthentication
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

internal fun Route.installNodePushRoutes(runtime: PushRuntime) {
    installNodeEnvelopeRoute(runtime)
    installNodeEnvelopeReplicaRoute(runtime)
    installNodeWakeUpRoute(runtime)
    installNodePendingRoute(runtime)
    installNodeAcknowledgementRoute(runtime)
}

private fun Route.installNodeEnvelopeRoute(runtime: PushRuntime) {
    post(NODE_ENVELOPE_PATH) {
        val body = call.receiveText()
        val envelope = runCatching { serverJson.decodeFromString<RelayEnvelope>(body) }.getOrNull()
        when {
            envelope == null ->
                call.respond(HttpStatusCode.BadRequest)

            !call.hasNodeRouteAccess(
                runtime = runtime,
                method = "POST",
                path = NODE_ENVELOPE_PATH,
                body = body,
                routingIds = setOf(envelope.senderId, envelope.recipientId)
            ) ->
                call.respond(HttpStatusCode.Unauthorized)

            else -> {
                val accepted = runtime.coordinator.accept(envelope)
                call.respond(
                    if (accepted) {
                        HttpStatusCode.Accepted
                    } else {
                        HttpStatusCode.InsufficientStorage
                    }
                )
            }
        }
    }
}

private fun Route.installNodeEnvelopeReplicaRoute(runtime: PushRuntime) {
    post(NODE_ENVELOPE_REPLICA_PATH) {
        val body = call.receiveText()
        val envelope = runCatching { serverJson.decodeFromString<RelayEnvelope>(body) }.getOrNull()
        when {
            envelope == null ->
                call.respond(HttpStatusCode.BadRequest)

            !call.hasNodeRouteAccess(
                runtime = runtime,
                method = "POST",
                path = NODE_ENVELOPE_REPLICA_PATH,
                body = body,
                routingIds = setOf(envelope.senderId, envelope.recipientId)
            ) ->
                call.respond(HttpStatusCode.Unauthorized)

            else -> {
                val accepted = runtime.coordinator.replicate(envelope)
                call.respond(
                    if (accepted) {
                        HttpStatusCode.Accepted
                    } else {
                        HttpStatusCode.InsufficientStorage
                    }
                )
            }
        }
    }
}

private fun Route.installNodeWakeUpRoute(runtime: PushRuntime) {
    post("/v1/node-push/wake-ups/{recipientId}") {
        val recipientId = call.parameters["recipientId"]
        val path = "/v1/node-push/wake-ups/$recipientId"
        when {
            recipientId.isNullOrBlank() ->
                call.respond(HttpStatusCode.BadRequest)

            !call.hasNodeAccess(runtime, "POST", path, "", NodeCapability.MAILBOX) ->
                call.respond(HttpStatusCode.Unauthorized)

            else -> {
                runtime.coordinator.notifyRecipient(recipientId)
                call.respond(HttpStatusCode.Accepted)
            }
        }
    }
}

private fun Route.installNodePendingRoute(runtime: PushRuntime) {
    get("/v1/node-push/recipients/{recipientId}/envelopes") {
        val recipientId = call.parameters["recipientId"]
        val path = "/v1/node-push/recipients/$recipientId/envelopes"
        when {
            recipientId.isNullOrBlank() ->
                call.respond(HttpStatusCode.BadRequest)

            !call.hasNodeRouteAccess(
                runtime = runtime,
                method = "GET",
                path = path,
                routingId = recipientId
            ) ->
                call.respond(HttpStatusCode.Unauthorized)

            else ->
                call.respond(
                    PendingRelayEnvelopesResponse(
                        runtime.pendingEnvelopes.pending(recipientId)
                    )
                )
        }
    }
}

private fun Route.installNodeAcknowledgementRoute(runtime: PushRuntime) {
    post("/v1/node-push/recipients/{recipientId}/envelopes/{envelopeId}/ack") {
        val recipientId = call.parameters["recipientId"]
        val envelopeId = call.parameters["envelopeId"]
        val path = "/v1/node-push/recipients/$recipientId/envelopes/$envelopeId/ack"
        val parametersValid = !recipientId.isNullOrBlank() && !envelopeId.isNullOrBlank()
        when {
            !parametersValid ->
                call.respond(HttpStatusCode.BadRequest)

            !call.hasNodeRouteAccess(
                runtime = runtime,
                method = "POST",
                path = path,
                routingId = requireNotNull(recipientId)
            ) ->
                call.respond(HttpStatusCode.Unauthorized)

            else -> {
                runtime.pendingEnvelopes.remove(
                    recipientId = requireNotNull(recipientId),
                    envelopeId = requireNotNull(envelopeId)
                )
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private suspend fun ApplicationCall.hasNodeRouteAccess(
    runtime: PushRuntime,
    method: String,
    path: String,
    routingId: String,
    body: String = ""
): Boolean =
    hasNodeRouteAccess(
        runtime = runtime,
        method = method,
        path = path,
        routingIds = setOf(routingId),
        body = body
    )

private suspend fun ApplicationCall.hasNodeRouteAccess(
    runtime: PushRuntime,
    method: String,
    path: String,
    routingIds: Set<String>,
    body: String = ""
): Boolean {
    val authentication = nodeRequestAuthentication()
    val nodeAuthorized =
        runtime.nodeRequestAuthorizer?.isAuthorized(
            authentication = authentication,
            method = method,
            path = path,
            body = body,
            requirements =
                NodeRequestAuthorizationRequirements(
                    requiredCapability = NodeCapability.GATEWAY
                )
        ) ?: false
    val ownsRoute =
        authentication?.nodeId?.let { nodeId ->
            routingIds.any { routingId ->
                runCatching {
                    runtime.nodeRouteOwnershipResolver?.isOwnedBy(routingId, nodeId) == true
                }.getOrDefault(false)
            }
        } ?: false
    return nodeAuthorized && ownsRoute
}

private suspend fun ApplicationCall.hasNodeAccess(
    runtime: PushRuntime,
    method: String,
    path: String,
    body: String,
    capability: NodeCapability
): Boolean =
    runtime.nodeRequestAuthorizer?.isAuthorized(
        authentication = nodeRequestAuthentication(),
        method = method,
        path = path,
        body = body,
        requirements =
            NodeRequestAuthorizationRequirements(
                requiredCapability = capability
            )
    ) ?: false

private const val NODE_ENVELOPE_PATH = "/v1/node-push/envelopes"
private const val NODE_ENVELOPE_REPLICA_PATH = "/v1/node-push/replicas/envelopes"
