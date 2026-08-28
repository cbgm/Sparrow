package com.cbgm.sparrow.feature.chats.presentation.verification.mapper

import com.cbgm.sparrow.core.extensions.toFingerprint
import com.cbgm.sparrow.feature.contactimport.presentation.scan.model.ScannedIdentityPreview
import com.cbgm.sparrow.feature.identity.domain.model.SharedIdentityPayload

internal fun SharedIdentityPayload.toScannedIdentityPreview(encodedIdentity: String): ScannedIdentityPreview =
    ScannedIdentityPreview(
        encodedIdentity = encodedIdentity,
        displayName = contactDetails.displayName,
        phoneNumber = contactDetails.phoneNumber,
        signingKeyFingerprint = signingPublicKey.toFingerprint(),
        encryptionKeyFingerprint = encryptionPublicKey.toFingerprint()
    )
