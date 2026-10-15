package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model

/** A copy of the Identity-owned pending member keys; no live DB entity is exposed. */
class PendingGroupIdentity(
    val encryptionPublicKey: ByteArray,
    val signingPublicKey: ByteArray
)
