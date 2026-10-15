package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model

import com.cbgm.sparrow.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPair

/** The local identity that signs group verification packets; its signing key pair matches [identity]. */
class GroupVerificationSigner(
    val identity: LocalPublicIdentity,
    val signingKeyPair: LocalSigningKeyPair
) {
    init {
        check(identity.signingPublicKey.contentEquals(signingKeyPair.publicKey)) {
            "Local signing key pair does not match the public identity"
        }
    }
}
