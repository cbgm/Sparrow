package com.cbgm.securechat.data.database.outbox

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.codec.PacketCodec
import com.cbgm.securechat.core.protocol.outbox.OutboxEvent
import com.cbgm.securechat.core.protocol.outbox.OutboxStateMachine
import com.cbgm.securechat.core.protocol.outbox.OutboxStatus
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutboxItem
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ProtocolOutboxDao
import com.cbgm.securechat.data.database.entity.ProtocolOutboxEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultProtocolOutbox(
    private val outboxDao: ProtocolOutboxDao,
    private val packetCodec: PacketCodec
) : ProtocolOutbox {
    override suspend fun enqueue(
        contactId: String,
        packet: SecureChatPacket
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
                return@runCatching existing.toDomain()
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
                    createdAtEpochMilliseconds = now,
                    updatedAtEpochMilliseconds = now
                )

            outboxDao.upsert(entity = entity)

            outboxDao
                .findByPacketId(packetId = packet.packetId)
                ?.toDomain()
                ?: error("Queued protocol packet could not be loaded")
        }
    }

    override fun observePending(): Flow<List<ProtocolOutboxItem>> =
        outboxDao
            .observePending()
            .map { entities ->
                entities.map { entity ->
                    entity.toDomain()
                }
            }

    override suspend fun getPending(limit: Int): Result<List<ProtocolOutboxItem>> =
        runCatching {
            require(limit > 0) {
                "Pending-item limit must be positive"
            }

            outboxDao.getPending(limit = limit).map { entity -> entity.toDomain() }
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

            outboxDao.findByPacketId(packetId = packetId)?.toDomain()
        }

    override suspend fun markSent(itemId: String): Result<Unit> =
        runCatching {
            require(itemId.isNotBlank()) {
                "Outbox item ID must not be blank"
            }

            val existing = outboxDao.findById(itemId = itemId) ?: error("Outbox item was not found")

            OutboxStateMachine.requireTransition(
                current = existing.status.toOutboxStatus(),
                event = OutboxEvent.SEND_SUCCEEDED
            )

            outboxDao.markSent(
                itemId = itemId,
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
                OutboxStatus.FAILED -> {
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

    private fun ProtocolOutboxEntity.toDomain(): ProtocolOutboxItem =
        ProtocolOutboxItem(
            id = id,
            contactId = contactId,
            packetId = packetId,
            encodedPacket = encodedPacket.copyOf(),
            status = status.toOutboxStatus(),
            attemptCount = attemptCount,
            lastError = lastError,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds,
            updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
        )

    private fun String.toOutboxStatus(): OutboxStatus =
        when (this) {
            OutboxStatus.PENDING.name -> OutboxStatus.PENDING

            OutboxStatus.PROCESSING.name -> OutboxStatus.PROCESSING

            OutboxStatus.SENT.name -> OutboxStatus.SENT

            OutboxStatus.FAILED.name -> OutboxStatus.FAILED

            else -> error("Unknown outbox status: $this")
        }

    private companion object {
        const val MAX_ERROR_LENGTH = 1_000
    }
}
