package com.cbgm.sparrow.feature.identity.domain.model

/** Verified handshake facts. Protocol packet parsing and signature checks belong to core:protocol
 * and are performed by orchestration before these values enter Identity.
 * Invite owns the corresponding user-visible invitation lifecycle separately.
 */
class IdentityExchangeOffer(
    val exchangeId: String,
    val createdAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long,
    inviteChallenge: ByteArray,
    encryptionPublicKey: ByteArray,
    signingPublicKey: ByteArray,
    val autoSharesIdentity: Boolean = false
) {
    val inviteChallenge = inviteChallenge.copyOf()
    val encryptionPublicKey = encryptionPublicKey.copyOf()
    val signingPublicKey = signingPublicKey.copyOf()
}

class IdentityExchangeAcceptance(
    val exchangeId: String,
    val acceptedAtEpochMilliseconds: Long,
    inviteChallenge: ByteArray,
    responseChallenge: ByteArray,
    inviterEncryptionPublicKey: ByteArray,
    inviterSigningPublicKey: ByteArray,
    responderEncryptionPublicKey: ByteArray,
    responderSigningPublicKey: ByteArray,
    val autoSharesIdentity: Boolean = false
) {
    val inviteChallenge = inviteChallenge.copyOf()
    val responseChallenge = responseChallenge.copyOf()
    val inviterEncryptionPublicKey = inviterEncryptionPublicKey.copyOf()
    val inviterSigningPublicKey = inviterSigningPublicKey.copyOf()
    val responderEncryptionPublicKey = responderEncryptionPublicKey.copyOf()
    val responderSigningPublicKey = responderSigningPublicKey.copyOf()
}

class IdentityExchangeReady(
    val exchangeId: String,
    responseChallenge: ByteArray,
    acceptedResponderEncryptionPublicKey: ByteArray,
    acceptedResponderSigningPublicKey: ByteArray,
    senderEncryptionPublicKey: ByteArray,
    senderSigningPublicKey: ByteArray
) {
    val responseChallenge = responseChallenge.copyOf()
    val acceptedResponderEncryptionPublicKey = acceptedResponderEncryptionPublicKey.copyOf()
    val acceptedResponderSigningPublicKey = acceptedResponderSigningPublicKey.copyOf()
    val senderEncryptionPublicKey = senderEncryptionPublicKey.copyOf()
    val senderSigningPublicKey = senderSigningPublicKey.copyOf()
}
