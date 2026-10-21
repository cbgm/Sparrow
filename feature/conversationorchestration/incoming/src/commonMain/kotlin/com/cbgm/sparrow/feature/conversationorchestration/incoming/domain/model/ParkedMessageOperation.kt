package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model

import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand

/** An operation that arrived before its message; [id] identifies it until it is applied. */
data class ParkedMessageOperation(
    val id: Long,
    val operation: IncomingCommand.ApplyRemoteMessageOperation
)
