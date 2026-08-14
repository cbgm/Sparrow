package com.cbgm.sparrow.feature.contactimport.presentation.scan.model

data class ScannedIdentityPreview(
    val encodedIdentity: String,
    val displayName: String?,
    val phoneNumber: String?,
    val signingKeyFingerprint: String,
    val encryptionKeyFingerprint: String
)
