package com.cbgm.sparrow.feature.membership.domain.model

/** Verified and decrypted by Membership. Do not resolve/mutate contacts before this result. */
data class OpenedIncomingGroupWelcome(
    val openedWelcome: OpenedGroupWelcomeDto,
    val localSigningPublicKey: ByteArray,
    val authoritySigningPublicKey: ByteArray,
    val authorityLeft: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpenedIncomingGroupWelcome) return false
        if (openedWelcome != other.openedWelcome) return false
        if (authorityLeft != other.authorityLeft) return false
        if (!localSigningPublicKey.contentEquals(other.localSigningPublicKey)) return false
        if (!authoritySigningPublicKey.contentEquals(other.authoritySigningPublicKey)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = openedWelcome.hashCode()
        result = 31 * result + authorityLeft.hashCode()
        result = 31 * result + localSigningPublicKey.contentHashCode()
        result = 31 * result + authoritySigningPublicKey.contentHashCode()
        return result
    }
}
