package com.cbgm.sparrow.feature.identity.data.model

internal class IdentityExchangeBindingDto(
    val exchangeId: String,
    val peerId: String,
    inviteChallenge: ByteArray,
    remoteSigningPublicKey: ByteArray
) {
    val inviteChallenge: ByteArray = inviteChallenge.copyOf()
    val remoteSigningPublicKey: ByteArray = remoteSigningPublicKey.copyOf()
}
