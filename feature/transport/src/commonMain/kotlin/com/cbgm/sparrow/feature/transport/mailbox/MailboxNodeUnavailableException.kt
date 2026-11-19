package com.cbgm.sparrow.feature.transport.mailbox

import com.cbgm.sparrow.feature.transport.connection.isRecoverableConnectivityFailure
import io.ktor.client.plugins.ServerResponseException

/**
 * The node holding a mailbox is down or unreachable (no connection, or a 5xx from its proxy). Expected
 * while a node is offline: the mailbox is read again on the next sync, and provisioning replaces it
 * once the node leaves the verified directory.
 */
class MailboxNodeUnavailableException(
    cause: Throwable
) : IllegalStateException("Mailbox node is unavailable", cause)

internal fun <T> Result<T>.markingNodeUnavailable(): Result<T> =
    exceptionOrNull()
        ?.takeIf { error -> error is ServerResponseException || error.isRecoverableConnectivityFailure() || error.isMailboxServerError() }
        ?.let { error -> Result.failure(MailboxNodeUnavailableException(error)) }
        ?: this

fun Throwable.isMailboxServerError(): Boolean {
    var current: Throwable? = this
    var depth = 0
    while (current != null && depth++ < 8) {
        if (current is MailboxNodeUnavailableException || current is ServerResponseException) return true
        if (current.isRecoverableConnectivityFailure()) return true
        val msg = current.message
        if (msg != null && (
                msg.contains("502") ||
                    msg.contains("500") ||
                    msg.contains("503") ||
                    msg.contains("504") ||
                    msg.contains("Bad Gateway") ||
                    msg.contains("Service Unavailable") ||
                    msg.contains("Gateway Timeout") ||
                    msg.contains("Mailbox retrieval failed") ||
                    msg.contains("Mailbox creation failed") ||
                    msg.contains("Mailbox delivery failed") ||
                    msg.contains("Mailbox acknowledgement failed")
            )
        ) {
            return true
        }
        current = current.cause
    }
    return false
}
