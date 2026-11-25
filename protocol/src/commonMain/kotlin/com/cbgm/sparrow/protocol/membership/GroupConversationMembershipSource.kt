package com.cbgm.sparrow.protocol.membership

import kotlinx.coroutines.flow.Flow

/** Membership-owned state of a group, so Chats can render the conversation without reading Membership's tables. */
interface GroupConversationMembershipSource {
    fun observeConversationMembership(groupId: String): Flow<GroupConversationMembershipSnapshot>
}
