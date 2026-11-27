package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.AliasClaimKeyResponse
import com.cbgm.sparrow.server.protocol.AliasClaimRequest
import com.cbgm.sparrow.server.protocol.AliasClaimResult
import com.cbgm.sparrow.server.protocol.ErrorResponse
import com.cbgm.sparrow.server.protocol.InvitationRouteLookup
import com.cbgm.sparrow.server.protocol.UnsignedAliasClaim
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.enforceRateLimit
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

internal fun Application.installPresenceRoutes(runtime: PresenceRuntime) {
    routing {
        installHealthRoute(runtime)
        installAliasClaimKey(runtime)
        installAliasClaims(runtime)
        installInvitationRoutes(runtime)
    }
}

private fun Route.installHealthRoute(runtime: PresenceRuntime) {
    get("/health") {
        call.respondText("ok persistence=${runtime.store.persistenceMode}")
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
