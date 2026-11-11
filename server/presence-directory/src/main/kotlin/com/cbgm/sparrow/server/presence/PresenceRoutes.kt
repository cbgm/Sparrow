package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.AliasClaimKeyResponse
import com.cbgm.sparrow.server.protocol.AliasClaimRequest
import com.cbgm.sparrow.server.protocol.AliasClaimResult
import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.ClientRoutingResult
import com.cbgm.sparrow.server.protocol.ErrorResponse
import com.cbgm.sparrow.server.protocol.InvitationRouteLookup
import com.cbgm.sparrow.server.protocol.NodeCapability
import com.cbgm.sparrow.server.protocol.UnsignedAliasClaim
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.InternalApiAuthentication
import com.cbgm.sparrow.server.security.NodeRequestAuthorizationRequirements
import com.cbgm.sparrow.server.security.enforceRateLimit
import com.cbgm.sparrow.server.security.nodeRequestAuthentication
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

internal fun Application.installPresenceRoutes(runtime: PresenceRuntime) {
    routing {
        installHealthRoute(runtime)
        installRouteRegistration(runtime)
        installRouteRemoval(runtime)
        installRouteResolution(runtime)
        installAliasClaimKey(runtime)
        installAliasClaims(runtime)
        installInvitationRoutes(runtime)
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
    respondToRegistration(runtime.store.register(registration))
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

/** The public key apps seal their alias claims for; the node registry signs it into the node directory. */
private fun Route.installAliasClaimKey(runtime: PresenceRuntime) {
    get("/v1/alias-claim-key") {
        call.respond(AliasClaimKeyResponse(publicKey = runtime.aliasClaimKeys.publicKey))
    }
}

/**
 * Apps claim their phone aliases here, through two relays ([com.cbgm.sparrow.server.protocol.RelayLayer]): the claim is
 * sealed for this directory and signed with the device key, so no node is involved and none can read it. The relay
 * address is shared by many apps, so the rate limit is per device.
 */
private fun Route.installAliasClaims(runtime: PresenceRuntime) {
    post("/v1/alias-claims") {
        val claim = runCatching { call.receive<AliasClaimRequest>() }.getOrNull()?.let { request -> runtime.aliasClaimKeys.open(request.sealedClaim) }
        when {
            claim == null ->
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ALIAS_CLAIM", "Alias claim rejected"))

            call.enforceRateLimit(runtime.aliasClaimRateLimiter, claim.routingId) -> call.bindAliases(claim, runtime)
        }
    }
}

/**
 * Senders look up where to put a packet for a phone alias or a device routing ID that has no mailbox route of its own
 * yet (a first invitation, or the answers before the contact's mailbox route arrived), through their circuit, so this
 * directory does not learn who asks. Anyone may ask, so every lookup is paid with the mailbox proof of work, and
 * nothing about it is kept.
 */
private fun Route.installInvitationRoutes(runtime: PresenceRuntime) {
    post("/v1/invitation-routes") {
        val lookup = runCatching { call.receive<InvitationRouteLookup>() }.getOrNull()?.takeIf { it.routingId.isLookedUp() }
        when {
            lookup == null -> call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_LOOKUP", "Invalid invitation route lookup"))
            !runtime.lookupWork.accepts(lookup.work) ->
                call.respond(HttpStatusCode.Forbidden, ErrorResponse("LOOKUP_WORK_REQUIRED", "A valid proof of work is required"))
            else -> call.respondInvitationRoute(lookup.routingId, runtime)
        }
    }
}

private fun String.isLookedUp(): Boolean = ClientRoutingIds.isBootstrapRoutingId(this) || ClientRoutingIds.isDeviceRoutingId(this)

private suspend fun ApplicationCall.respondInvitationRoute(
    routingId: String,
    runtime: PresenceRuntime
) {
    when (val result = runtime.store.invitationRoute(routingId)) {
        null -> respond(HttpStatusCode.NotFound, ErrorResponse("NO_INVITATION_ROUTE", "No invitation route"))
        else -> respond(result)
    }
}

private suspend fun ApplicationCall.bindAliases(
    claim: UnsignedAliasClaim,
    runtime: PresenceRuntime
) {
    when (val claims = runtime.phoneVerification.claims(claim)) {
        null ->
            respond(
                HttpStatusCode.Forbidden,
                ErrorResponse("PHONE_VERIFICATION_FAILED", "Phone verification failed")
            )

        else -> {
            val bound = runtime.store.bindAliases(claim.routingId, claims)
            runtime.store.saveInvitationRoute(claim.routingId, claim.invitationRoute)
            respond(AliasClaimResult(acceptedAliasIndexes = claim.aliases.indices.filter { index -> claim.aliases[index] in bound }))
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
        PresenceResult.Accepted -> respond(HttpStatusCode.NoContent)

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
