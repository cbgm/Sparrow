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
