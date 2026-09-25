package com.cbgm.sparrow.feature.chats.data.direct.outgoing

/**
 * Automatic release/read-receipt work is opportunistic. Pending authorization is
 * normal during startup/onboarding and must never manufacture an outgoing packet
 * or be logged as an exception. Explicit user sends still use the strict gate.
 *
 * Only the missing-authorization reason defers work; unexpected repository,
 * database or transport exceptions propagate to the caller.
 */
internal class DirectAutomaticOutgoingGate(
    private val authorizationBlockReason: suspend (contactId: String) -> String?
) {
    suspend fun run(
        contactId: String,
        hasPendingWork: Boolean,
        operation: suspend () -> Unit
    ): Boolean {
        if (!hasPendingWork) return false
        if (authorizationBlockReason(contactId) != null) return false
        operation()
        return true
    }
}
