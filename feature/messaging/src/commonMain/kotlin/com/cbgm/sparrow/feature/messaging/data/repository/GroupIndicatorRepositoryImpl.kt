package com.cbgm.sparrow.feature.messaging.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.chats.domain.model.IndicatorType
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupIndicatorRepository
import com.cbgm.sparrow.feature.messaging.data.datasource.GroupRoutingDataSource
import com.cbgm.sparrow.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

class GroupIndicatorRepositoryImpl(
    private val webSocketTransportClient: WebSocketTransportClient,
    private val groupRoutingDataSource: GroupRoutingDataSource
) : GroupIndicatorRepository {
    override fun observeMember(
        groupId: String,
        contactId: String
    ): Flow<IndicatorType> =
        webSocketTransportClient.incomingIndicatorEvents
            .transform { event ->
                val routingId =
                    try {
                        groupRoutingDataSource.resolve(groupId, contactId)
                    } catch (_: Throwable) {
                        return@transform
                    }
                if (event.senderId == routingId) {
                    emit(event.indicatorType.toIndicatorType())
                }
            }

    override suspend fun setIndicator(
        groupId: String,
        indicatorType: IndicatorType
    ): Result<Unit> =
        safeSuspendCall {
            val recipientRoutingIds = groupRoutingDataSource.resolveIndicatorMembers(groupId).values

            var firstFailure: Throwable? = null
            recipientRoutingIds.forEach { routingId ->
                webSocketTransportClient
                    .sendIndicatorState(
                        recipientId = routingId,
                        indicatorType = indicatorType.name
                    ).exceptionOrNull()
                    ?.let { error ->
                        if (firstFailure == null) firstFailure = error
                    }
            }
            firstFailure?.let { error -> throw error }
        }
}

private fun String.toIndicatorType(): IndicatorType =
    IndicatorType.entries.firstOrNull { it.name == this } ?: IndicatorType.NONE
