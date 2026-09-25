package com.cbgm.sparrow.feature.identity.domain.model

/** Identity-owned exchange facts; no invitation/conversation decision is made here. */
class IdentityExchangeBinding(
    val exchangeId: String,
    val peerId: String,
    inviteChallenge: ByteArray,
    remoteSigningPublicKey: ByteArray
) {
    val inviteChallenge: ByteArray = inviteChallenge.copyOf()
    val remoteSigningPublicKey: ByteArray = remoteSigningPublicKey.copyOf()
}
