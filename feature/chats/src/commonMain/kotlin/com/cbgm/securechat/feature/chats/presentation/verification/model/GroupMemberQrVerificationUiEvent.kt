package com.cbgm.securechat.feature.chats.presentation.verification.model

sealed interface GroupMemberQrVerificationUiEvent {
    data class QrCodeScanned(
        val encodedIdentity: String
    ) : GroupMemberQrVerificationUiEvent

    data object BackClicked : GroupMemberQrVerificationUiEvent

    data object ConfirmClicked : GroupMemberQrVerificationUiEvent

    data object PreviewDismissed : GroupMemberQrVerificationUiEvent

    data object RetryClicked : GroupMemberQrVerificationUiEvent
}
