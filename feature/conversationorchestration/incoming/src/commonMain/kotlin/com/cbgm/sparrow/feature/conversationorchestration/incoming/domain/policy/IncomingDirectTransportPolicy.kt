package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy

import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode

/**
 * Attachments and message operations of a direct conversation are only accepted over the encrypted Sparrow
 * transport. The two checks fail differently: an attachment over another transport is invalid content
 * (IllegalArgumentException), an operation over it a broken invariant (IllegalStateException).
 */
object IncomingDirectTransportPolicy {
    fun isEncrypted(transportMode: String): Boolean = transportMode == TransportEncryptionMode.SEALED_BOX.name

    fun requireAttachmentsAllowed(transportMode: String, hasAttachments: Boolean) {
        require(!hasAttachments || isEncrypted(transportMode)) {
            "Direct message attachments require an encrypted Sparrow transport"
        }
    }

    fun checkOperationAllowed(transportMode: String) {
        check(isEncrypted(transportMode)) { "Direct message operations require an encrypted Sparrow transport" }
    }
}
