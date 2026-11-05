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
        ?.takeIf { error -> error is ServerResponseException || error.isRecoverableConnectivityFailure() }
        ?.let { error -> Result.failure(MailboxNodeUnavailableException(error)) }
        ?: this
