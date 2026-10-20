package com.cbgm.sparrow.feature.conversationorchestration.delivery.data.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity

internal class MessageConversationDataSource(
    private val chatDao: ChatDao
) {
    suspend fun findMessage(messageId: String): MessageEntity? = chatDao.findMessageById(messageId)

    suspend fun findConversation(conversationId: String): ConversationEntity? =
        chatDao.findConversationById(conversationId)
}
