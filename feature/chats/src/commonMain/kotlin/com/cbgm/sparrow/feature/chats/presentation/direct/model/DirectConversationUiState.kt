package com.cbgm.sparrow.feature.chats.presentation.direct.model

import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.chats.domain.model.direct.ContactSecurityState
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectComposerState
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi

data class DirectConversationUiState(
    val contactId: String = "",
    val contactName: String = "",
    val profilePictureBytes: ByteArray? = null,
    val messages: List<MessageBubbleUi> = emptyList(),
    val contactSecurityState: ContactSecurityState = ContactSecurityState.NO_REMOTE_PUBLIC_KEYS,
    val identitySetupMode: DirectIdentitySetupMode = DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING,
    val isLoading: Boolean = true,
    val isChatAuthorized: Boolean = false,
    val composerState: DirectComposerState = DirectComposerState.DISABLED
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DirectConversationUiState) return false

        return contactId == other.contactId &&
            contactName == other.contactName &&
            profilePictureBytes.contentEquals(other.profilePictureBytes) &&
            messages == other.messages &&
            contactSecurityState == other.contactSecurityState &&
            identitySetupMode == other.identitySetupMode &&
            isLoading == other.isLoading &&
            isChatAuthorized == other.isChatAuthorized &&
            composerState == other.composerState
    }

    override fun hashCode(): Int {
        var result = contactId.hashCode()
        result = 31 * result + contactName.hashCode()
        result = 31 * result + (profilePictureBytes?.contentHashCode() ?: 0)
        result = 31 * result + messages.hashCode()
        result = 31 * result + contactSecurityState.hashCode()
        result = 31 * result + identitySetupMode.hashCode()
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + isChatAuthorized.hashCode()
        result = 31 * result + composerState.hashCode()
        return result
    }
}
