package com.cbgm.sparrow.feature.transport.data.mailbox

import com.cbgm.sparrow.feature.transport.domain.model.MailboxGoneException
import com.cbgm.sparrow.feature.transport.domain.model.MailboxNodeUnavailableException
import com.cbgm.sparrow.feature.transport.domain.rule.isRecoverableConnectivityFailure
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode

/** A 5xx from the node's proxy means the node is down, a missing mailbox that it is gone; anything else is an error. */
internal fun HttpResponse.checkMailboxStatus(
    action: String,
    accepted: Boolean
) {
    when {
        accepted -> Unit
        status in NODE_DOWN -> throw MailboxNodeUnavailableException(IllegalStateException("$action failed with $status"))
        status in MAILBOX_GONE -> throw MailboxGoneException(status.value)
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
