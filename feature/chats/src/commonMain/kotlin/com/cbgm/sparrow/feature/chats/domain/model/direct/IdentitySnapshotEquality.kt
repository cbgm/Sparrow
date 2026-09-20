package com.cbgm.sparrow.feature.chats.domain.model.direct

import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity

/** ByteArray keys are compared by value so unrelated Identity updates do not refresh this chat. */
internal fun RemotePeerIdentity?.hasSameIdentityContent(other: RemotePeerIdentity?): Boolean {
    if (this == null || other == null) return this == null && other == null
    return peerId == other.peerId &&
        encryptionPublicKey.contentEquals(other.encryptionPublicKey) &&
        signingPublicKey.contentEquals(other.signingPublicKey) &&
        verificationStatus == other.verificationStatus &&
        keyExchangeStatus == other.keyExchangeStatus &&
        verifiedByContact == other.verifiedByContact &&
        locallyImported == other.locallyImported &&
        updatedAtEpochMilliseconds == other.updatedAtEpochMilliseconds
}
