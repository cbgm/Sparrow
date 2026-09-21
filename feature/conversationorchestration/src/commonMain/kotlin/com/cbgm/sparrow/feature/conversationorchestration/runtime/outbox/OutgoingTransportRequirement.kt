package com.cbgm.sparrow.feature.conversationorchestration.runtime.outbox

data class OutgoingTransportRequirement(
    val requiresEncryption: Boolean,
    val allowsEncryptionBeforeMutualIdentity: Boolean = false,
    val forcePlaintext: Boolean = false,
    val encryptionUnavailableMessage: String =
        "This protocol packet requires an encrypted Sparrow transport"
)
