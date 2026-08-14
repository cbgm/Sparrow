package com.cbgm.sparrow.feature.chats.domain.model.group

data class GroupConversation(
    val id: String,
    val title: String,
    val messages: List<GroupMessage>,
    val unreadCount: Int,
    val participantContactIds: List<String>,
    val pendingParticipantCount: Int,
    val isReady: Boolean,
    val state: GroupConversationState,
    val isIncomingInvitation: Boolean,
    val memberInvitationStates: List<GroupMemberInvitationState>
) {
    val lastMessage: GroupMessage?
        get() = messages.maxByOrNull(GroupMessage::timestamp)
}
