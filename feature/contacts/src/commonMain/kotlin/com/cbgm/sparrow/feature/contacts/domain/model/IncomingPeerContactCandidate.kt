package com.cbgm.sparrow.feature.contacts.domain.model

/** A cryptographically authenticated packet member whose contact record may need resolution. */
data class IncomingPeerContactCandidate(
    val signingPublicKey: ByteArray,
    val phoneNumber: String?
)
