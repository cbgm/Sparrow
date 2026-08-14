package com.cbgm.sparrow.feature.chats.data.storage

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.feature.chats.data.direct.storage.DirectConversationStorage
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus

class UnreadableTransportMessageStorage(
    private val chatDao: ChatDao,
    private val conversationStorage: DirectConversationStorage
) {
    suspend fun store(
        contactId: String,
        encodedTransportPayload: String,
        text: String,
        transportMode: String,
        contentStatus: MessageContentStatus,
        receivedAtEpochMilliseconds: Long
    ) {
        val conversation = conversationStorage.getOrCreate(contactId)
        chatDao.upsertMessage(
            MessageEntity(
                id = IdGenerator.generate(prefix = "failed-message"),
                conversationId = conversation.id,
                packetId = null,
                text = text,
                transportPayload = encodedTransportPayload,
                transportMode = transportMode,
                contentStatus = contentStatus.name,
                deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE.name,
                senderContactId = conversation.contactId,
                isMine = false,
                createdAtEpochMilliseconds = receivedAtEpochMilliseconds
            )
        )
        chatDao.updateConversationTimestamp(conversation.id, receivedAtEpochMilliseconds)
    }
}
