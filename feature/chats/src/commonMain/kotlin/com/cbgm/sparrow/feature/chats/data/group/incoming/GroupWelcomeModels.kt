package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity

internal data class PreviousGroupMembershipDto(
    val participants: List<ConversationParticipantEntity>,
    val signingKeysByContactId: Map<String, ByteArray?>
)
