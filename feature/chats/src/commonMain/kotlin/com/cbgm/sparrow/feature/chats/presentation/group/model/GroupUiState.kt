package com.cbgm.sparrow.feature.chats.presentation.group.model

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMemberInvitationStatus
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection

data class GroupMemberProgressUi(
    val displayName: String,
    val status: GroupMemberInvitationStatus
)

data class GroupUiState(
    val title: String = "",
    val avatarBytes: ByteArray? = null,
    val messages: List<GroupMessageUiModel> = emptyList(),
    val messageText: String = "",
    val selectedMedia: List<MediaSelection> = emptyList(),
    val isSending: Boolean = false,
    val isSomeoneTyping: Boolean = false,
    val typingDisplayName: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = true,
    val isMessageInputEnabled: Boolean = false,
    val state: GroupConversationState = GroupConversationState.READY,
    val memberCount: Int = 0,
    val readyMemberCount: Int = 0,
    val pendingMemberCount: Int = 0,
    val showInvitationActions: Boolean = false,
    val memberProgress: List<GroupMemberProgressUi> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GroupUiState

        if (isSending != other.isSending) return false
        if (isSomeoneTyping != other.isSomeoneTyping) return false
        if (isLoading != other.isLoading) return false
        if (isMessageInputEnabled != other.isMessageInputEnabled) return false
        if (memberCount != other.memberCount) return false
        if (readyMemberCount != other.readyMemberCount) return false
        if (pendingMemberCount != other.pendingMemberCount) return false
        if (showInvitationActions != other.showInvitationActions) return false
        if (title != other.title) return false
        if (!avatarBytes.contentEquals(other.avatarBytes)) return false
        if (messages != other.messages) return false
        if (messageText != other.messageText) return false
        if (selectedMedia != other.selectedMedia) return false
        if (typingDisplayName != other.typingDisplayName) return false
        if (errorMessage != other.errorMessage) return false
        if (state != other.state) return false
        if (memberProgress != other.memberProgress) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isSending.hashCode()
        result = 31 * result + isSomeoneTyping.hashCode()
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + isMessageInputEnabled.hashCode()
        result = 31 * result + memberCount
        result = 31 * result + readyMemberCount
        result = 31 * result + pendingMemberCount
        result = 31 * result + showInvitationActions.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (avatarBytes?.contentHashCode() ?: 0)
        result = 31 * result + messages.hashCode()
        result = 31 * result + messageText.hashCode()
        result = 31 * result + selectedMedia.hashCode()
        result = 31 * result + typingDisplayName.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + state.hashCode()
        result = 31 * result + memberProgress.hashCode()
        return result
    }
}
