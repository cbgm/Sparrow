package com.cbgm.securechat.feature.chats.presentation.verification.model

import com.cbgm.securechat.feature.contactimport.presentation.scan.model.ScannedIdentityPreview

data class GroupMemberQrVerificationUiState(
    val scanAttempt: Int = 0,
    val preview: ScannedIdentityPreview? = null,
    val isProcessing: Boolean = false,
    val isVerified: Boolean = false,
    val error: GroupMemberQrVerificationError? = null
)

enum class GroupMemberQrVerificationError {
    INVALID_QR,
    IDENTITY_MISMATCH,
    VERIFICATION_FAILED
}
