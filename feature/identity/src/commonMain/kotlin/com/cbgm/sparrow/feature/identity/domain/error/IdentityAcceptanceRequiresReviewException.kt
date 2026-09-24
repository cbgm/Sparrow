package com.cbgm.sparrow.feature.identity.domain.error

/**
 * A signed response to current invitation presents keys other than those captured
 * for the existing contact. Its signature authenticates the new key only; the old
 * contact binding must remain untouched until the recipient explicitly accepts
 * the proposal shown in Mailbox. Accepting does not cryptographically prove
 * continuity with the old identity.
 */
class IdentityAcceptanceRequiresReviewException(
    val peerId: String,
    val invitationExpiresAtEpochMilliseconds: Long
) : IllegalStateException("Invitation response presents a different contact identity; recipient approval required")
