package com.cbgm.sparrow.feature.transport.mailbox

import com.cbgm.sparrow.core.util.result.ExpectedFailure
import com.cbgm.sparrow.feature.transport.connection.isRecoverableConnectivityFailure
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode

/**
 * The node holding a mailbox is down or unreachable (no connection, or a 5xx from its proxy). Expected
 * while a node is offline: the mailbox is read again on the next sync, and provisioning replaces it
 * once the node leaves the verified directory.
 */
class MailboxNodeUnavailableException(
    cause: Throwable
) : IllegalStateException("Mailbox node is unavailable", cause),
    ExpectedFailure

/** The node no longer knows this mailbox (it expired, was revoked, or the node was installed anew). */
class MailboxGoneException(
    status: HttpStatusCode
) : IllegalStateException("Mailbox is gone ($status)")

/** A 5xx from the node's proxy means the node is down, a missing mailbox that it is gone; anything else is an error. */
internal fun HttpResponse.checkMailboxStatus(
    action: String,
    accepted: Boolean
) {
    when {
        accepted -> Unit
        status in NODE_DOWN -> throw MailboxNodeUnavailableException(IllegalStateException("$action failed with $status"))
        status in MAILBOX_GONE -> throw MailboxGoneException(status)
        else -> error("$action failed with $status")
    }
}

/** The mailbox no longer exists for this capability; revoking it again can never succeed. */
internal val MAILBOX_GONE = setOf(HttpStatusCode.NotFound, HttpStatusCode.Gone, HttpStatusCode.Unauthorized)

private val NODE_DOWN = setOf(HttpStatusCode.BadGateway, HttpStatusCode.ServiceUnavailable, HttpStatusCode.GatewayTimeout)

internal fun <T> Result<T>.markingNodeUnavailable(): Result<T> =
    exceptionOrNull()
        ?.takeIf { error -> error is ServerResponseException || error.isRecoverableConnectivityFailure() }
        ?.let { error -> Result.failure(MailboxNodeUnavailableException(error)) }
        ?: this
