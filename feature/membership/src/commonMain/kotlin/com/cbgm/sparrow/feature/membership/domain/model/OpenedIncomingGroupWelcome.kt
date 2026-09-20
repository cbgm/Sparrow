package com.cbgm.sparrow.feature.membership.domain.model

/** Verified and decrypted by Membership. Do not resolve/mutate contacts before this result. */
data class OpenedIncomingGroupWelcome(
    val openedWelcome: OpenedGroupWelcomeDto,
    val localSigningPublicKey: ByteArray,
    val authoritySigningPublicKey: ByteArray,
    val authorityLeft: Boolean
)
