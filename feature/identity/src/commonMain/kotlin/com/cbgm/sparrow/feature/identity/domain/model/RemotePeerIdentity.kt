package com.cbgm.sparrow.feature.identity.domain.model

/** Identity-owned read model. The persisted keys are never exposed as mutable database entities. */
class RemotePeerIdentity(
    val peerId: String,
    val encryptionPublicKey: ByteArray,
    val signingPublicKey: ByteArray,
    val verificationStatus: ContactVerificationStatus,
    val keyExchangeStatus: KeyExchangeStatus,
    val verifiedByContact: Boolean,
    val locallyImported: Boolean,
    val updatedAtEpochMilliseconds: Long
)

/** Whether these are exactly the given keys; differing keys are a key change, never the same identity. */
fun RemotePeerIdentity.hasKeys(encryptionPublicKey: ByteArray, signingPublicKey: ByteArray): Boolean =
    this.encryptionPublicKey.contentEquals(encryptionPublicKey) && this.signingPublicKey.contentEquals(signingPublicKey)

/**
 * Direct message content must never fall back to plaintext when contact keys or
 * mutual authorization are absent. Verification is intentionally independent:
 * a mutually established but UNVERIFIED identity can still be encrypted.
 */
fun RemotePeerIdentity?.hasDirectMessageEncryptionKeys(): Boolean =
    this != null &&
        keyExchangeStatus == KeyExchangeStatus.MUTUAL &&
        encryptionPublicKey.size == 32 &&
        signingPublicKey.size == 32
