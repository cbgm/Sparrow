package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.repository

import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.ParkedMessageOperation

/**
 * Operations whose message has not arrived yet, for example an edit that overtook its message after a
 * reconnect. They wait until the message is stored and are dropped after [RETENTION_MILLISECONDS], the
 * lifetime of an undelivered envelope.
 */
interface ParkedMessageOperationRepository {
    suspend fun park(operation: IncomingCommand.ApplyRemoteMessageOperation): Result<Unit>

    /** The operations waiting for [messageId], in the order they were sent. */
    suspend fun findFor(messageId: String): Result<List<ParkedMessageOperation>>

    suspend fun remove(id: Long): Result<Unit>

    companion object {
        const val RETENTION_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
    }
}
