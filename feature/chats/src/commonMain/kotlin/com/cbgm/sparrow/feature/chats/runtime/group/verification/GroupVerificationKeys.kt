package com.cbgm.sparrow.feature.chats.runtime.group.verification

import com.cbgm.sparrow.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPair

internal fun requireMatchingSigningKey(
    identity: LocalPublicIdentity,
    signingKeyPair: LocalSigningKeyPair
) {
    check(identity.signingPublicKey.contentEquals(signingKeyPair.publicKey)) {
        "Local signing key pair does not match the public identity"
    }
}
