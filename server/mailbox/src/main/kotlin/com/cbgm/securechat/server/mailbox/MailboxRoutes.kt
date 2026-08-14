package com.cbgm.securechat.server.mailbox

import com.cbgm.securechat.server.protocol.CreateMailboxRequest
import com.cbgm.securechat.server.protocol.EnvelopeAcceptanceState
import com.cbgm.securechat.server.protocol.ErrorResponse
import com.cbgm.securechat.server.protocol.FederationAcknowledgement
import com.cbgm.securechat.server.protocol.MailboxEnvelopeRequest
import com.cbgm.securechat.server.protocol.MailboxEnvelopesResponse
import com.cbgm.securechat.server.security.BoundedRateLimiter
import com.cbgm.securechat.server.security.enforceRateLimit
import com.cbgm.securechat.server.security.hashedClientAddress
import com.cbgm.securechat.server.security.respondTooManyRequests
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
    pushNotifier: MailboxPushNotifier,
    creationRateLimiter: BoundedRateLimiter
) {
    routing {
        get("/health") { call.respondMailboxHealth(store) }
        post("/v1/mailboxes") {
            call.createMailbox(config, store, creationRateLimiter)
        }
        post("/v1/mailboxes/{mailboxId}/envelopes") {
            call.storeEnvelope(store, pushNotifier)
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
    creationRateLimiter: BoundedRateLimiter
) {
    if (!enforceRateLimit(creationRateLimiter)) {
        return
    }

    when (
        val result =
            store.createWithQuota(
                request = receive<CreateMailboxRequest>(),
                ownerKeyHash = hashedClientAddress(),
                maximumMailboxes = config.maximumMailboxes,
                maximumMailboxesPerOwner = config.maximumMailboxesPerClient
            )
    ) {
        is MailboxCreationResult.Created -> respond(HttpStatusCode.Created, result.response)
        MailboxCreationResult.OwnerQuotaExceeded -> respondOwnerQuotaExceeded(config)
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

private suspend fun ApplicationCall.respondOwnerQuotaExceeded(config: MailboxConfig) {
    respondTooManyRequests(
        retryAfterSeconds =
            (config.creationRateLimit.windowMilliseconds / MILLISECONDS_PER_SECOND)
                .coerceAtLeast(MINIMUM_RETRY_AFTER_SECONDS),
        code = "MAILBOX_CLIENT_QUOTA_EXCEEDED",
        message = "Active mailbox quota exceeded"
    )
}

private suspend fun ApplicationCall.storeEnvelope(
    store: MailboxStorage,
    pushNotifier: MailboxPushNotifier
) {
    val mailboxId = parameters["mailboxId"]
    val capability = bearerCapability()
    when {
        mailboxId == null -> respond(HttpStatusCode.BadRequest)
        capability == null -> respond(HttpStatusCode.Unauthorized)
        else -> storeAuthorizedEnvelope(mailboxId, capability, store, pushNotifier)
    }
}

private suspend fun ApplicationCall.storeAuthorizedEnvelope(
    mailboxId: String,
    capability: String,
    store: MailboxStorage,
    pushNotifier: MailboxPushNotifier
) {
    val request = receive<MailboxEnvelopeRequest>()
    when (val result = store.store(mailboxId, capability, request.envelope)) {
        is MailboxResult.Stored -> {
            if (!result.duplicate) {
                runCatching {
                    pushNotifier.notify(request.envelope.recipientDeviceRoutingId)
                }
            }
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
                respond(MailboxEnvelopesResponse(pending))
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

private const val MILLISECONDS_PER_SECOND = 1_000L
private const val MINIMUM_RETRY_AFTER_SECONDS = 1L
