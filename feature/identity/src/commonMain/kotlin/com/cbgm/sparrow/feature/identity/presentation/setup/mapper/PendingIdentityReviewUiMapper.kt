package com.cbgm.sparrow.feature.identity.presentation.setup.mapper

import com.cbgm.sparrow.core.extensions.toFingerprint
import com.cbgm.sparrow.feature.identity.domain.model.PendingRemoteIdentityChange
import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import com.cbgm.sparrow.feature.identity.presentation.setup.model.PendingIdentityReviewUi

/** Fingerprints are display-only; they are NOT proof of ownership of the old identity or a phone number. */
internal fun PendingRemoteIdentityChange.toReviewUi(previous: RemotePeerIdentity?): PendingIdentityReviewUi =
    PendingIdentityReviewUi(
        peerId = peerId,
        invitationId = invitationId,
        previousSigningKey = previous?.signingPublicKey?.toFingerprint(),
        proposedSigningKey = proposedSigningPublicKey.toFingerprint(),
        previousEncryptionKey = previous?.encryptionPublicKey?.toFingerprint(),
        proposedEncryptionKey = proposedEncryptionPublicKey.toFingerprint()
    )
