package com.cbgm.sparrow.feature.contacts.domain.model

/** A cryptographically authenticated packet member whose contact record may need resolution. */
data class IncomingPeerContactCandidate(
    val signingPublicKey: ByteArray,
    val phoneNumber: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IncomingPeerContactCandidate) return false
        if (phoneNumber != other.phoneNumber) return false
        if (!signingPublicKey.contentEquals(other.signingPublicKey)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = (phoneNumber?.hashCode() ?: 0)
        result = 31 * result + signingPublicKey.contentHashCode()
        return result
    }
}
