package com.cbgm.sparrow.data.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity

data class ConversationWithMessages(
    @Embedded
    val conversation: ConversationEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "conversationId"
    )
    val messages: List<MessageEntity>
)
