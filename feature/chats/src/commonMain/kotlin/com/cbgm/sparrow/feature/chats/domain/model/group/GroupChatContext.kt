package com.cbgm.sparrow.feature.chats.domain.model.group

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.membership.domain.model.GroupAdministrationState

data class GroupChatContext(
    val conversation: GroupConversation?,
    val conversationError: Throwable?,
    val administration: GroupAdministrationState,
    val contacts: List<Contact>,
    val pin: GroupPin?
)
