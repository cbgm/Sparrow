package com.cbgm.sparrow.feature.conversationorchestration.incoming.data.mapper

import com.cbgm.sparrow.data.database.entity.ParkedMessageOperationEntity
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.OperationConversation
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.ParkedMessageOperation
import com.cbgm.sparrow.protocol.message.MessageOperation
import kotlinx.serialization.json.Json

private val operationJson = Json { ignoreUnknownKeys = true }

internal fun IncomingCommand.ApplyRemoteMessageOperation.toParkedEntity(
    parkedAtEpochMilliseconds: Long
): ParkedMessageOperationEntity =
    ParkedMessageOperationEntity(
        targetMessageId = operation.messageId,
        conversationId = conversationId,
        conversation = conversation.name,
        senderContactId = senderContactId,
        operation = operationJson.encodeToString(MessageOperation.serializer(), operation),
        sentAtEpochMilliseconds = sentAtEpochMilliseconds,
        parkedAtEpochMilliseconds = parkedAtEpochMilliseconds
    )

internal fun ParkedMessageOperationEntity.toDomain(): ParkedMessageOperation =
    ParkedMessageOperation(
        id = id,
        operation =
            IncomingCommand.ApplyRemoteMessageOperation(
                conversationId = conversationId,
                senderContactId = senderContactId,
                operation = operationJson.decodeFromString(MessageOperation.serializer(), operation),
                conversation = OperationConversation.valueOf(conversation),
                sentAtEpochMilliseconds = sentAtEpochMilliseconds
            )
    )
