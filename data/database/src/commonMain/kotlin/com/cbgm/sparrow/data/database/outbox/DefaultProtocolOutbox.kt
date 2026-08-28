package com.cbgm.sparrow.data.database.outbox

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.codec.PacketCodec
import com.cbgm.sparrow.core.protocol.outbox.OutboxEvent
import com.cbgm.sparrow.core.protocol.outbox.OutboxStateMachine
import com.cbgm.sparrow.core.protocol.outbox.OutboxStatus
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutboxItem
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ProtocolOutboxDao
import com.cbgm.sparrow.data.database.entity.ProtocolOutboxEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultProtocolOutbox(
    private val outboxDao: ProtocolOutboxDao,
    private val packetCodec: PacketCodec
) : ProtocolOutbox {
    override suspend fun enqueue(
        contactId: String,
        packet: SparrowPacket
    ): Result<ProtocolOutboxItem> {
        return runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            require(packet.packetId.isNotBlank()) {
                "Packet ID must not be blank"
            }

            val existing = outboxDao.findByPacketId(packetId = packet.packetId)

            if (existing != null) {
                return@runCatching existing.toProtocolOutboxItem()
            }

            val encodedPacket = packetCodec.encode(packet = packet).getOrThrow()

            val now = SystemClock.nowEpochMilliseconds()

            val entity =
                ProtocolOutboxEntity(
                    id = IdGenerator.generate(prefix = "outbox"),
                    contactId = contactId,
                    packetId = packet.packetId,
                    encodedPacket = encodedPacket,
                    status = OutboxStatus.PENDING.name,
                    attemptCount = 0,
                    lastError = null,
                    expiresAtEpochMilliseconds = null,
                    createdAtEpochMilliseconds = now,
                    updatedAtEpochMilliseconds = now
                )

            outboxDao.upsert(entity = entity)

            outboxDao
                .findByPacketId(packetId = packet.packetId)
                ?.toProtocolOutboxItem()
                ?: error("Queued protocol packet could not be loaded")
        }
    }

    override fun observePending(): Flow<List<ProtocolOutboxItem>> =
        outboxDao
            .observePending()
            .map { entities ->
                entities.map { entity ->
                    entity.toProtocolOutboxItem()
                }
            }

    override fun observeNextSentExpiry(): Flow<Long?> =
        outboxDao.observeNextSentExpiry()

    override suspend fun expireSent(nowEpochMilliseconds: Long): Result<List<ProtocolOutboxItem>> =
        runCatching {
            require(nowEpochMilliseconds >= 0L) { "Current time must not be negative" }

            outboxDao.findExpiredSent(nowEpochMilliseconds).mapNotNull { entity ->
                OutboxStateMachine.requireTransition(
                    current = entity.status.toOutboxStatus(),
                    event = OutboxEvent.DELIVERY_EXPIRED
                )

                val updated =
                    outboxDao.markExpired(
                        itemId = entity.id,
                        errorMessage = DELIVERY_EXPIRED_ERROR,
                        updatedAt = nowEpochMilliseconds
                    )
                if (updated == 0) {
                    null
                } else {
                    entity
                        .copy(
                            status = OutboxStatus.EXPIRED.name,
                            lastError = DELIVERY_EXPIRED_ERROR,
                            updatedAtEpochMilliseconds = nowEpochMilliseconds
                        ).toProtocolOutboxItem()
                }
            }
        }

    override suspend fun getPending(limit: Int): Result<List<ProtocolOutboxItem>> =
        runCatching {
            require(limit > 0) {
                "Pending-item limit must be positive"
            }

            outboxDao.getPending(limit = limit).map { entity -> entity.toProtocolOutboxItem() }
        }

    override suspend fun markProcessing(itemId: String): Result<Unit> =
        runCatching {
            require(itemId.isNotBlank()) {
                "Outbox item ID must not be blank"
            }

            val existing = outboxDao.findById(itemId = itemId) ?: error("Outbox item was not found")

            OutboxStateMachine.requireTransition(
                current = existing.status.toOutboxStatus(),
                event = OutboxEvent.PROCESSING_STARTED
            )

            outboxDao.markProcessing(
                itemId = itemId,
                updatedAt = SystemClock.nowEpochMilliseconds()
            )
        }

    override suspend fun requeueInterrupted(): Result<Unit> =
        runCatching {
            outboxDao.requeueInterrupted(
                updatedAt = SystemClock.nowEpochMilliseconds()
            )
        }

    override suspend fun retryFailed(): Result<Unit> =
        runCatching {
            outboxDao.retryFailed(
                updatedAt = SystemClock.nowEpochMilliseconds()
            )
        }

    override suspend fun findByPacketId(packetId: String): Result<ProtocolOutboxItem?> =
        runCatching {
            require(packetId.isNotBlank()) {
                "Packet ID must not be blank"
            }

            outboxDao.findByPacketId(packetId = packetId)?.toProtocolOutboxItem()
        }

    override suspend fun markSent(itemId: String): Result<Unit> =
        markSent(
            itemId = itemId,
            expiresAtEpochMilliseconds = Long.MAX_VALUE
        )

    override suspend fun markSent(
        itemId: String,
        expiresAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            require(itemId.isNotBlank()) {
                "Outbox item ID must not be blank"
            }
            require(expiresAtEpochMilliseconds > SystemClock.nowEpochMilliseconds()) {
                "Server delivery deadline must be in the future"
            }

            val existing = outboxDao.findById(itemId = itemId) ?: error("Outbox item was not found")

            OutboxStateMachine.requireTransition(
                current = existing.status.toOutboxStatus(),
                event = OutboxEvent.SEND_SUCCEEDED
            )

            outboxDao.markSent(
                itemId = itemId,
                expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
                updatedAt = SystemClock.nowEpochMilliseconds()
            )
        }

    override suspend fun markFailed(
        itemId: String,
        errorMessage: String
    ): Result<Unit> =
        runCatching {
            require(itemId.isNotBlank()) {
                "Outbox item ID must not be blank"
            }

            require(errorMessage.isNotBlank()) {
                "Error message must not be blank"
            }

            val existing = outboxDao.findById(itemId = itemId) ?: error("Outbox item was not found")

            OutboxStateMachine.requireTransition(
                current = existing.status.toOutboxStatus(),
                event = OutboxEvent.SEND_FAILED
            )

            outboxDao.markFailed(
                itemId = itemId,
                errorMessage = errorMessage.take(MAX_ERROR_LENGTH),
                updatedAt = SystemClock.nowEpochMilliseconds()
            )
        }

    override suspend fun retry(itemId: String): Result<Unit> =
        runCatching {
            require(itemId.isNotBlank()) {
                "Outbox item ID must not be blank"
            }

            val existing = outboxDao.findById(itemId = itemId) ?: error("Outbox item was not found")

            OutboxStateMachine.requireTransition(
                current = existing.status.toOutboxStatus(),
                event = OutboxEvent.RETRY_REQUESTED
            )

            outboxDao.retry(
                itemId = itemId,
                updatedAt = SystemClock.nowEpochMilliseconds()
            )
        }

    override suspend fun resend(packetId: String): Result<Unit> =
        runCatching {
            require(packetId.isNotBlank()) {
                "Packet ID must not be blank"
            }

            val existing = outboxDao.findByPacketId(packetId = packetId) ?: return@runCatching

            when (existing.status.toOutboxStatus()) {
                OutboxStatus.PENDING,
                OutboxStatus.PROCESSING -> Unit

                OutboxStatus.SENT,
                OutboxStatus.FAILED,
                OutboxStatus.EXPIRED -> {
                    val updatedRows =
                        outboxDao.requeueForResend(
                            packetId = packetId,
                            updatedAt = SystemClock.nowEpochMilliseconds()
                        )
                    if (updatedRows == 0) {
                        val refreshed = outboxDao.findByPacketId(packetId = packetId)
                        check(
                            refreshed != null &&
                                refreshed.status.toOutboxStatus() in
                                setOf(OutboxStatus.PENDING, OutboxStatus.PROCESSING)
                        ) {
                            "Outbox packet could not be re-queued for resend"
                        }
                    }
                }
            }
        }

    private fun ProtocolOutboxEntity.toProtocolOutboxItem(): ProtocolOutboxItem =
        ProtocolOutboxItem(
            id = id,
            contactId = contactId,
            packetId = packetId,
            encodedPacket = encodedPacket.copyOf(),
            status = status.toOutboxStatus(),
            attemptCount = attemptCount,
            lastError = lastError,
            expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds,
            updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
        )

    private fun String.toOutboxStatus(): OutboxStatus =
        when (this) {
            OutboxStatus.PENDING.name -> OutboxStatus.PENDING

            OutboxStatus.PROCESSING.name -> OutboxStatus.PROCESSING

            OutboxStatus.SENT.name -> OutboxStatus.SENT

            OutboxStatus.FAILED.name -> OutboxStatus.FAILED

            OutboxStatus.EXPIRED.name -> OutboxStatus.EXPIRED

            else -> error("Unknown outbox status: $this")
        }

    private companion object {
        const val MAX_ERROR_LENGTH = 1_000
        const val DELIVERY_EXPIRED_ERROR = "Server delivery retention expired"
    }
}
