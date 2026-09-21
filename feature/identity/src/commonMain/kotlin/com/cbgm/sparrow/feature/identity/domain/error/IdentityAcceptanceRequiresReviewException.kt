package com.cbgm.sparrow.feature.identity.domain.error

/**
 * A signed response to OUR current invitation presents keys other than those captured
 * for the existing contact. Its signature authenticates the new key only; the old
 * contact binding must remain untouched until explicit, independent verification.
 */
class IdentityAcceptanceRequiresReviewException(
    val peerId: String,
    val invitationExpiresAtEpochMilliseconds: Long
) : IllegalStateException("Invitation response presents a different contact identity; independent review required")
