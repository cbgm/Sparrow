package com.cbgm.sparrow.feature.conversationorchestration.api.observer

import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationIndicator
import kotlinx.coroutines.flow.Flow

/** Read-only view of transient transport indicators; they are never persisted, so Chats cannot read them elsewhere. */
interface ConversationIndicatorObserver {
    fun observeContact(contactId: String): Flow<ConversationIndicator>

    fun observeGroupMember(groupId: String, contactId: String): Flow<ConversationIndicator>
}
