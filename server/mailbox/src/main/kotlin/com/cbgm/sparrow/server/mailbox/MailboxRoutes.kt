package com.cbgm.sparrow.server.mailbox

import com.cbgm.sparrow.server.protocol.CreateMailboxRequest
import com.cbgm.sparrow.server.protocol.EnvelopeAcceptanceState
import com.cbgm.sparrow.server.protocol.ErrorResponse
import com.cbgm.sparrow.server.protocol.FederatedEnvelope
import com.cbgm.sparrow.server.protocol.FederationAcknowledgement
import com.cbgm.sparrow.server.protocol.MailboxEnvelopeRequest
import com.cbgm.sparrow.server.protocol.MailboxEnvelopesResponse
import com.cbgm.sparrow.server.protocol.MailboxWaitRequest
import com.cbgm.sparrow.server.protocol.MailboxWaitResponse
import com.cbgm.sparrow.server.security.CreationWorkCheck
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

internal fun Application.configureMailboxRoutes(
    config: MailboxConfig,
    store: MailboxStorage,
    creationWork: CreationWorkCheck,
    arrivals: MailboxArrivals = MailboxArrivals()
) {
    routing {
        get("/health") { call.respondMailboxHealth(store) }
        post("/v1/mailboxes") {
            call.createMailbox(config, store, creationWork)
        }
        post("/v1/mailboxes/wait") {
            call.respondReadyMailboxes(store, arrivals)
        }
        post("/v1/mailboxes/{mailboxId}/envelopes") {
            call.storeEnvelope(store, arrivals, creationWork)
        }
        get("/v1/mailboxes/{mailboxId}/envelopes") {
            call.respondPendingEnvelopes(store)
        }
        delete("/v1/mailboxes/{mailboxId}/envelopes/{envelopeId}") {
            call.acknowledgeEnvelope(store)
        }
        delete("/v1/mailboxes/{mailboxId}") { call.revokeMailbox(store) }
    }
}

private suspend fun ApplicationCall.respondMailboxHealth(store: MailboxStorage) {
    respondText("ok persistence=${store.persistenceMode} mailboxes=${store.mailboxCount()}")
}

private suspend fun ApplicationCall.createMailbox(
    config: MailboxConfig,
    store: MailboxStorage,
    creationWork: CreationWorkCheck
) {
    val request = receive<CreateMailboxRequest>()
    if (!creationWork.accepts(request.creationWork)) {
        respond(HttpStatusCode.Forbidden, ErrorResponse("MAILBOX_WORK_REQUIRED", "A valid proof of work is required"))
        return
    }
    // Nothing about the creator is kept: the proof of work is the price, so there is no per-address quota.
    when (
        val result =
            store.createWithQuota(
                request = MailboxLifetimes.capped(request.copy(creationWork = null), System.currentTimeMillis()),
                ownerKeyHash = UNATTRIBUTED_OWNER,
                maximumMailboxes = config.maximumMailboxes,
                maximumMailboxesPerOwner = Int.MAX_VALUE
            )
    ) {
        is MailboxCreationResult.Created -> respond(HttpStatusCode.Created, result.response)
        MailboxCreationResult.OwnerQuotaExceeded,
        MailboxCreationResult.GlobalQuotaExceeded ->
            respond(
                HttpStatusCode.InsufficientStorage,
                ErrorResponse(
                    code = "MAILBOX_GLOBAL_QUOTA_EXCEEDED",
                    message = "Mailbox service capacity exhausted"
                )
            )
    }
}

private suspend fun ApplicationCall.respondReadyMailboxes(
    store: MailboxStorage,
    arrivals: MailboxArrivals
) {
    val request = runCatching { receive<MailboxWaitRequest>() }.getOrNull()
    when {
        request == null || request.mailboxes.size > MAXIMUM_WATCHED_MAILBOXES -> respond(HttpStatusCode.BadRequest)
        else -> respond(MailboxWaitResponse(arrivals.ready(store, request.mailboxes, WAIT_MILLISECONDS)))
    }
}

private suspend fun ApplicationCall.storeEnvelope(
    store: MailboxStorage,
    arrivals: MailboxArrivals,
    depositWork: CreationWorkCheck
) {
    val mailboxId = parameters["mailboxId"]
    val capability = bearerCapability()
    when {
        mailboxId == null -> respond(HttpStatusCode.BadRequest)
        capability == null -> respond(HttpStatusCode.Unauthorized)
        else -> storeAuthorizedEnvelope(mailboxId, capability, store, arrivals, depositWork)
    }
}

private suspend fun ApplicationCall.storeAuthorizedEnvelope(
    mailboxId: String,
    capability: String,
    store: MailboxStorage,
    arrivals: MailboxArrivals,
    depositWork: CreationWorkCheck
) {
    val request = receive<MailboxEnvelopeRequest>()
    val envelope =
        runCatching { MailboxLifetimes.capped(request.envelope, System.currentTimeMillis()) }.getOrElse {
            respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ENVELOPE", "Envelope rejected"))
            return
        }
    // Only an invitation mailbox needs the work; checking it for every deposit would spend stamps for nothing.
    val depositPaid = request.depositWork?.let(depositWork::accepts) == true
    when (val result = store.store(mailboxId, capability, envelope, depositPaid)) {
        is MailboxResult.Stored -> {
            if (!result.duplicate) arrivals.stored(mailboxId)
            respond(
                HttpStatusCode.Accepted,
                FederationAcknowledgement(
                    envelopeId = request.envelope.envelopeId,
                    state = EnvelopeAcceptanceState.STORED_AT_DESTINATION,
                    duplicate = result.duplicate
                )
            )
        }

        is MailboxResult.Rejected ->
            respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(result.code, "Envelope rejected")
            )
    }
}

private suspend fun ApplicationCall.respondPendingEnvelopes(store: MailboxStorage) {
    val mailboxId = parameters["mailboxId"]
    val capability = bearerCapability()
    when {
        mailboxId == null -> respond(HttpStatusCode.BadRequest)
        capability == null -> respond(HttpStatusCode.Unauthorized)
        else -> {
            val pending = store.pending(mailboxId, capability)
            if (pending == null) {
                respond(HttpStatusCode.Unauthorized)
            } else {
                respond(MailboxEnvelopesResponse(pending.firstPage()))
            }
        }
    }
}

private suspend fun ApplicationCall.acknowledgeEnvelope(store: MailboxStorage) {
    val mailboxId = parameters["mailboxId"]
    val envelopeId = parameters["envelopeId"]
    val capability = bearerCapability()
    when {
        mailboxId == null || envelopeId == null -> respond(HttpStatusCode.BadRequest)
        capability == null -> respond(HttpStatusCode.Unauthorized)
        store.acknowledge(mailboxId, capability, envelopeId) ->
            respond(HttpStatusCode.NoContent)

        else -> respond(HttpStatusCode.Unauthorized)
    }
}

private suspend fun ApplicationCall.revokeMailbox(store: MailboxStorage) {
    val mailboxId = parameters["mailboxId"]
    val capability = bearerCapability()
    when {
        mailboxId == null -> respond(HttpStatusCode.BadRequest)
        capability == null -> respond(HttpStatusCode.Unauthorized)
        else -> respondRevocation(store.revoke(mailboxId, capability))
    }
}

private suspend fun ApplicationCall.respondRevocation(result: MailboxRevocationResult) {
    when (result) {
        MailboxRevocationResult.Revoked,
        MailboxRevocationResult.NotFound -> respond(HttpStatusCode.NoContent)

        MailboxRevocationResult.Unauthorized -> respond(HttpStatusCode.Unauthorized)
    }
}

/**
 * The envelopes of one answer: in order, until about [PAGE_BYTES] of payload, but always at least one. The owner
 * acknowledges them and asks again for the rest, so an answer always fits through a circuit.
 */
private fun List<FederatedEnvelope>.firstPage(): List<FederatedEnvelope> {
    var bytes = 0
    return takeWhile { envelope ->
        val fits = bytes == 0 || bytes + envelope.encryptedPayload.length <= PAGE_BYTES
        bytes += envelope.encryptedPayload.length
        fits
    }
}

private const val PAGE_BYTES = 1_024 * 1_024

/** Below the session node's 10 s exit timeout, so a wait through a circuit always gets its answer. */
private const val WAIT_MILLISECONDS = 8_000L
private const val MAXIMUM_WATCHED_MAILBOXES = 512
