package com.cbgm.sparrow.feature.identity.domain.model

/** Verified invite signature, NOT verified continuity with the previously trusted keys. */
class PendingRemoteIdentityChange(
    val peerId: String,
    val sourcePeerId: String,
    val invitationId: String,
    val proposedEncryptionPublicKey: ByteArray,
    val proposedSigningPublicKey: ByteArray,
    val receivedAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long
)
