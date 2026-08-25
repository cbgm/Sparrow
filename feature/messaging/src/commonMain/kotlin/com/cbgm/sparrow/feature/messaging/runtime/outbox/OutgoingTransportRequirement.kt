package com.cbgm.sparrow.feature.messaging.runtime.outbox

data class OutgoingTransportRequirement(
    val requiresEncryption: Boolean,
    val allowsEncryptionBeforeMutualIdentity: Boolean = false,
    val forcePlaintext: Boolean = false,
    val encryptionUnavailableMessage: String =
        "This protocol packet requires an encrypted Sparrow transport"
)
