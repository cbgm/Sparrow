package com.cbgm.sparrow.feature.identity.domain.model

/** Identity facts only; callers decide which state permits a conversation. */
data class IdentityPeerState(
    val hasEstablishedExchange: Boolean,
    val hasMutualIdentity: Boolean,
    /**
     * An invitation to or from this peer is still in flight (sent, or accepted and waiting for READY): packets
     * the peer sent after the exchange completed on its side can arrive before that completion here.
     */
    val hasPendingExchange: Boolean = false
)
