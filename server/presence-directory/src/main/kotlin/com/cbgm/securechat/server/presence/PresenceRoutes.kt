package com.cbgm.securechat.server.presence

import com.cbgm.securechat.server.protocol.ClientRouteRegistration
import com.cbgm.securechat.server.protocol.ErrorResponse
import com.cbgm.securechat.server.protocol.NodeCapability
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.security.NodeRequestAuthorizationRequirements
import com.cbgm.securechat.server.security.nodeRequestAuthentication
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

internal fun Application.installPresenceRoutes(runtime: PresenceRuntime) {
    routing {
        installHealthRoute(runtime)
        installRouteRegistration(runtime)
        installRouteRemoval(runtime)
        installRouteResolution(runtime)
    }
}

private fun Route.installHealthRoute(runtime: PresenceRuntime) {
    get("/health") {
        call.respondText(
            "ok persistence=${runtime.store.persistenceMode} " +
                "routes=${runtime.store.routeCount()}"
        )
    }
}

private fun Route.installRouteRegistration(runtime: PresenceRuntime) {
    put("/v1/routes/{routingId}") {
        val body = call.receiveText()
        val registration =
            runCatching { serverJson.decodeFromString<ClientRouteRegistration>(body) }
                .getOrNull()
        val routingId = call.parameters["routingId"]
        val authorized =
            registration?.let { routeRegistration ->
                call.isAuthorizedRegistration(
                    registration = routeRegistration,
                    routingId = routingId,
                    body = body,
                    runtime = runtime
                )
            } ?: false

        when {
            registration == null ->
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("INVALID_ROUTE", "Invalid route registration")
                )

            registration.route.routingId != routingId ->
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("ROUTING_ID_MISMATCH", "Path and body differ")
                )

            !authorized ->
                call.respond(HttpStatusCode.Unauthorized)

            else ->
                call.respondToRegistration(runtime.store.register(registration))
        }
    }
}

private suspend fun ApplicationCall.isAuthorizedRegistration(
    registration: ClientRouteRegistration,
    routingId: String?,
    body: String,
    runtime: PresenceRuntime
): Boolean =
    runtime.nodeRequestAuthorizer.isAuthorized(
        authentication = nodeRequestAuthentication(),
        method = "PUT",
        path = "/v1/routes/$routingId",
        body = body,
        requirements =
            NodeRequestAuthorizationRequirements(
                expectedNodeId = registration.route.nodeId,
                requiredCapability = NodeCapability.GATEWAY
            )
    )

private fun Route.installRouteRemoval(runtime: PresenceRuntime) {
    delete("/v1/routes/{routingId}/{connectionId}") {
        val routeKey = call.routeKey()
        when {
            routeKey == null ->
                call.respond(HttpStatusCode.BadRequest)

            !call.isAuthorizedRemoval(routeKey, runtime) ->
                call.respond(HttpStatusCode.Unauthorized)

            else -> {
                runtime.store.remove(routeKey.routingId, routeKey.connectionId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun Route.installRouteResolution(runtime: PresenceRuntime) {
    get("/v1/routes/{routingId}") {
        val routingId = call.parameters["routingId"]
        if (routingId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest)
        } else {
            call.respond(runtime.store.resolve(routingId))
        }
    }
}

private suspend fun ApplicationCall.respondToRegistration(result: PresenceResult) {
    when (result) {
        PresenceResult.Accepted ->
            respond(HttpStatusCode.NoContent)

        is PresenceResult.Rejected ->
            respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(result.code, "Route rejected")
            )
    }
}

private fun ApplicationCall.routeKey(): PresenceRouteKey? {
    val routingId = parameters["routingId"]
    val connectionId = parameters["connectionId"]
    return routingId?.let { resolvedRoutingId ->
        connectionId?.let { resolvedConnectionId ->
            PresenceRouteKey(
                routingId = resolvedRoutingId,
                connectionId = resolvedConnectionId
            )
        }
    }
}

private suspend fun ApplicationCall.isAuthorizedRemoval(
    routeKey: PresenceRouteKey,
    runtime: PresenceRuntime
): Boolean {
    val registeredNodeId =
        runtime.store
            .resolve(routeKey.routingId)
            .routes
            .firstOrNull { route -> route.connectionId == routeKey.connectionId }
            ?.nodeId
    val path = "/v1/routes/${routeKey.routingId}/${routeKey.connectionId}"

    return runtime.nodeRequestAuthorizer.isAuthorized(
        authentication = nodeRequestAuthentication(),
        method = "DELETE",
        path = path,
        body = "",
        requirements =
            NodeRequestAuthorizationRequirements(
                expectedNodeId = registeredNodeId,
                requiredCapability = NodeCapability.GATEWAY
            )
    )
}

private data class PresenceRouteKey(
    val routingId: String,
    val connectionId: String
)
