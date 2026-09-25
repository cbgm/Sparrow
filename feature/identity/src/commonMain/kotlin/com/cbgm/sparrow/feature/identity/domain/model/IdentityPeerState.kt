package com.cbgm.sparrow.feature.identity.domain.model

/** Identity facts only; callers decide which state permits a conversation. */
data class IdentityPeerState(
    val hasEstablishedExchange: Boolean,
    val hasMutualIdentity: Boolean
)
