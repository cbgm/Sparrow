package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.ClientRouteRegistrationResult
import com.cbgm.sparrow.server.protocol.ClientRoutingResult
import com.cbgm.sparrow.server.protocol.ErrorResponse
import com.cbgm.sparrow.server.protocol.NodeCapability
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.InternalApiAuthentication
import com.cbgm.sparrow.server.security.NodeRequestAuthorizationRequirements
import com.cbgm.sparrow.server.security.nodeRequestAuthentication
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

            else -> call.registerRoute(registration, runtime)
        }
    }
}

private suspend fun ApplicationCall.registerRoute(
    registration: ClientRouteRegistration,
    runtime: PresenceRuntime
) {
    when (val claims = runtime.phoneVerification.claims(registration)) {
        null ->
            respond(
                HttpStatusCode.Forbidden,
                ErrorResponse("PHONE_VERIFICATION_FAILED", "Phone verification failed")
            )

        else ->
            respondToRegistration(
                runtime.store.register(registration.copy(phoneVerificationToken = null), claims)
            )
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

/**
 * Only nodes (signed request) and the control plane's push service (internal token) resolve routes. Open lookups
 * would let anyone hash a list of phone numbers and learn who uses Sparrow, their device and when they are online.
 * Aliases are left out: a node needs the routes, not the phone aliases of a device.
 */
private fun Route.installRouteResolution(runtime: PresenceRuntime) {
    get("/v1/routes/{routingId}") {
        val routingId = call.parameters["routingId"]
        when {
            routingId.isNullOrBlank() -> call.respond(HttpStatusCode.BadRequest)
            !call.isAuthorizedResolution(routingId, runtime) -> call.respond(HttpStatusCode.Unauthorized)
            else -> call.respond(runtime.store.resolve(routingId).withoutAliases())
        }
    }
}

private suspend fun ApplicationCall.isAuthorizedResolution(
    routingId: String,
    runtime: PresenceRuntime
): Boolean {
    val token = runtime.internalApiToken
    val presented = request.headers[InternalApiAuthentication.TOKEN_HEADER]
    if (token != null && presented != null) {
        return InternalApiAuthentication.isAuthorized(token, presented)
    }
    return runtime.nodeRequestAuthorizer.isAuthorized(
        authentication = nodeRequestAuthentication(),
        method = "GET",
        path = "/v1/routes/$routingId",
        body = "",
        requirements = NodeRequestAuthorizationRequirements(requiredCapability = NodeCapability.FEDERATION)
    )
}

private fun ClientRoutingResult.withoutAliases(): ClientRoutingResult =
    copy(routes = routes.map { route -> route.copy(aliases = null) })

private suspend fun ApplicationCall.respondToRegistration(result: PresenceResult) {
    when (result) {
        is PresenceResult.Accepted ->
            respond(ClientRouteRegistrationResult(aliases = result.aliases))

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
