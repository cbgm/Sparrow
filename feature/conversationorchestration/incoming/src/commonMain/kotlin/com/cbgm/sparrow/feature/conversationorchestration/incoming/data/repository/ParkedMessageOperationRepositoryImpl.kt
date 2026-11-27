package com.cbgm.sparrow.feature.conversationorchestration.incoming.data.repository

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.incoming.data.datasource.ParkedMessageOperationDataSource
import com.cbgm.sparrow.feature.conversationorchestration.incoming.data.mapper.toDomain
import com.cbgm.sparrow.feature.conversationorchestration.incoming.data.mapper.toParkedEntity
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.ParkedMessageOperation
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.repository.ParkedMessageOperationRepository

internal class ParkedMessageOperationRepositoryImpl(
    private val dataSource: ParkedMessageOperationDataSource
) : ParkedMessageOperationRepository {
    override suspend fun park(operation: IncomingCommand.ApplyRemoteMessageOperation): Result<Unit> =
        safeSuspendCall {
            val now = SystemClock.nowEpochMilliseconds()
            // Operations whose message never arrived expire like their envelope would.
            dataSource.deleteParkedBefore(now - ParkedMessageOperationRepository.RETENTION_MILLISECONDS)
            dataSource.insert(operation.toParkedEntity(now))
        }

    override suspend fun findFor(messageId: String): Result<List<ParkedMessageOperation>> =
        safeSuspendCall { dataSource.findByTarget(messageId).map { it.toDomain() } }

    override suspend fun remove(id: Long): Result<Unit> = safeSuspendCall { dataSource.delete(id) }
}
