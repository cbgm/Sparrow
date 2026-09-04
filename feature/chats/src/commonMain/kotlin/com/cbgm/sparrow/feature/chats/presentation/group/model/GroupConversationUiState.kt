package com.cbgm.sparrow.feature.chats.presentation.group.model

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupComposerState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversationState
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi

data class GroupConversationUiState(
    val title: String = "",
    val avatarBytes: ByteArray? = null,
    val messages: List<MessageBubbleUi> = emptyList(),
    val isLoading: Boolean = true,
    val state: GroupConversationState = GroupConversationState.READY,
    val composerState: GroupComposerState = GroupComposerState.DISABLED
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupConversationUiState) return false

        return title == other.title &&
            avatarBytes.contentEquals(other.avatarBytes) &&
            messages == other.messages &&
            isLoading == other.isLoading &&
            state == other.state &&
            composerState == other.composerState
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + (avatarBytes?.contentHashCode() ?: 0)
        result = 31 * result + messages.hashCode()
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + composerState.hashCode()
        return result
    }
}
