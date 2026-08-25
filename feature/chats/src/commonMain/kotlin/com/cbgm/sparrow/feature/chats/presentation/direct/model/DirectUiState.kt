package com.cbgm.sparrow.feature.chats.presentation.direct.model

import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.attachments.presentation.model.GalleryMediaSelection
import com.cbgm.sparrow.feature.chats.domain.model.direct.ContactSecurityState
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleModel

data class DirectUiState(
    val contactId: String = "",
    val contactName: String = "",
    val profilePictureBytes: ByteArray? = null,
    val messages: List<MessageBubbleModel> = emptyList(),
    val messageText: String = "",
    val selectedGalleryMedia: List<GalleryMediaSelection> = emptyList(),
    val isSending: Boolean = false,
    val isContactTyping: Boolean = false,
    val contactSecurityState: ContactSecurityState = ContactSecurityState.NO_REMOTE_PUBLIC_KEYS,
    val identitySetupMode: DirectIdentitySetupMode = DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING,
    val isLoading: Boolean = true,
    val isChatAuthorized: Boolean = false,
    val composerState: DirectComposerState = DirectComposerState.DISABLED,
    val errorMessage: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DirectUiState

        if (isSending != other.isSending) return false
        if (isContactTyping != other.isContactTyping) return false
        if (isLoading != other.isLoading) return false
        if (isChatAuthorized != other.isChatAuthorized) return false
        if (contactId != other.contactId) return false
        if (contactName != other.contactName) return false
        if (!profilePictureBytes.contentEquals(other.profilePictureBytes)) return false
        if (messages != other.messages) return false
        if (messageText != other.messageText) return false
        if (selectedGalleryMedia != other.selectedGalleryMedia) return false
        if (contactSecurityState != other.contactSecurityState) return false
        if (identitySetupMode != other.identitySetupMode) return false
        if (composerState != other.composerState) return false
        if (errorMessage != other.errorMessage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isSending.hashCode()
        result = 31 * result + isContactTyping.hashCode()
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + isChatAuthorized.hashCode()
        result = 31 * result + contactId.hashCode()
        result = 31 * result + contactName.hashCode()
        result = 31 * result + (profilePictureBytes?.contentHashCode() ?: 0)
        result = 31 * result + messages.hashCode()
        result = 31 * result + messageText.hashCode()
        result = 31 * result + selectedGalleryMedia.hashCode()
        result = 31 * result + contactSecurityState.hashCode()
        result = 31 * result + identitySetupMode.hashCode()
        result = 31 * result + composerState.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        return result
    }
}
