package com.cbgm.sparrow.feature.messaging.runtime.outbox

import com.cbgm.sparrow.core.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.sparrow.core.protocol.outbox.OutboxProcessingResult
import com.cbgm.sparrow.core.protocol.outbox.OutboxProcessor
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutboxItem
import com.cbgm.sparrow.core.time.SystemClock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class DefaultOutboxProcessor(
    private val protocolOutbox: ProtocolOutbox,
    private val packetSender: OutgoingPacketSender,
    private val deliveryStateListener: OutboxDeliveryStateListener
) : OutboxProcessor {
    override suspend fun processPending(limit: Int): Result<OutboxProcessingResult> =
        runCatching {
            require(limit > 0) { "Outbox processing limit must be positive" }
            val results = processByRecipient(protocolOutbox.getPending(limit).getOrThrow())
            OutboxProcessingResult(
                processedCount = results.size,
                sentCount = results.count { result -> result.isSuccess },
                failedCount = results.count { result -> result.isFailure }
            )
        }

    override suspend fun expireAccepted(): Result<Int> =
        runCatching {
            val expiredItems =
                protocolOutbox
                    .expireSent(SystemClock.nowEpochMilliseconds())
                    .getOrThrow()
            expiredItems.forEach { item ->
                deliveryStateListener.onExpired(item.packetId).getOrThrow()
            }
            expiredItems.size
        }

    private suspend fun processByRecipient(
        pendingItems: List<ProtocolOutboxItem>
    ): List<Result<Unit>> =
        coroutineScope {
            val slots = Semaphore(MAX_CONCURRENT_RECIPIENTS)
            pendingItems
                .groupBy(ProtocolOutboxItem::contactId)
                .values
                .map { recipientItems ->
                    async {
                        slots.withPermit {
                            recipientItems.map { item -> processItem(item) }
                        }
                    }
                }.awaitAll()
                .flatten()
        }

    private suspend fun processItem(item: ProtocolOutboxItem): Result<Unit> {
        protocolOutbox.markProcessing(item.id).getOrElse { error ->
            return Result.failure(error)
        }

        val sendResult =
            runCatching {
                deliveryStateListener.onProcessing(item.packetId).getOrThrow()
                packetSender.send(item).getOrThrow()
            }
        if (sendResult.isFailure) {
            return markFailed(item, sendResult.exceptionOrNull())
        }

        val acceptance = sendResult.getOrThrow()
        protocolOutbox
            .markSent(item.id, acceptance.expiresAtEpochMilliseconds)
            .getOrElse { error -> return Result.failure(error) }
        return deliveryStateListener.onSent(item.packetId)
    }

    private suspend fun markFailed(
        item: ProtocolOutboxItem,
        error: Throwable?
    ): Result<Unit> {
        val errorMessage = error?.message ?: "Outgoing packet could not be sent"
        protocolOutbox
            .markFailed(item.id, errorMessage)
            .getOrElse { markFailedError -> return Result.failure(markFailedError) }
        deliveryStateListener
            .onFailed(item.packetId, errorMessage)
            .getOrElse { listenerError -> return Result.failure(listenerError) }
        return Result.failure(error ?: IllegalStateException(errorMessage))
    }

    private companion object {
        const val MAX_CONCURRENT_RECIPIENTS = 8
    }
}
