package com.cbgm.sparrow.feature.identity.data.model

/** Persisted outgoing exchange encountered a signed response with new remote keys. */
internal class IdentityAcceptanceReviewRequiredDtoException(
    val peerId: String,
    val invitationExpiresAtEpochMilliseconds: Long
) : IllegalStateException("A signed invitation response presents changed remote keys")
