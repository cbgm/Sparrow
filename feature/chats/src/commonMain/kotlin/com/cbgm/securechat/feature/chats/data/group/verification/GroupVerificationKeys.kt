package com.cbgm.securechat.feature.chats.data.group.verification

import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentity
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPair

internal fun requireMatchingSigningKey(
    identity: LocalPublicIdentity,
    signingKeyPair: LocalSigningKeyPair
) {
    check(identity.signingPublicKey.contentEquals(signingKeyPair.publicKey)) {
        "Local signing key pair does not match the public identity"
    }
}
