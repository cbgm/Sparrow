package com.cbgm.securechat.server.federation

import com.cbgm.securechat.server.protocol.EnvelopeAcceptanceState
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import java.util.concurrent.ConcurrentHashMap

data class OutboundEnvelopeEntry(
    val envelope: FederatedEnvelope,
    val state: EnvelopeAcceptanceState,
    val attempts: Int,
    val nextAttemptAtEpochMilliseconds: Long
)

interface OutboundEnvelopeStorage : AutoCloseable {
    val persistenceMode: String

    suspend fun enqueue(envelope: FederatedEnvelope): OutboundEnvelopeEntry

    suspend fun bindRecipient(
        envelopeId: String,
        recipientDeviceRoutingId: String
    ): OutboundEnvelopeEntry?

    suspend fun markAttempt(
        envelopeId: String,
        nextAttemptAtEpochMilliseconds: Long
    ): OutboundEnvelopeEntry?

    suspend fun markStored(envelopeId: String)

    suspend fun get(envelopeId: String): OutboundEnvelopeEntry?

    suspend fun pendingDue(
        nowEpochMilliseconds: Long,
        limit: Int
    ): List<OutboundEnvelopeEntry>

    suspend fun pendingCount(): Int
}

class OutboundEnvelopeQueue(
    private val now: () -> Long = System::currentTimeMillis
) : OutboundEnvelopeStorage {
    private val entries = ConcurrentHashMap<String, OutboundEnvelopeEntry>()

    override val persistenceMode: String = "memory"

    override suspend fun enqueue(envelope: FederatedEnvelope): OutboundEnvelopeEntry {
        purgeExpired()
        return entries.computeIfAbsent(envelope.envelopeId) {
            OutboundEnvelopeEntry(
                envelope = envelope,
                state = EnvelopeAcceptanceState.QUEUED_AT_GATEWAY,
                attempts = 0,
                nextAttemptAtEpochMilliseconds = now()
            )
        }
    }

    override suspend fun bindRecipient(
        envelopeId: String,
        recipientDeviceRoutingId: String
    ): OutboundEnvelopeEntry? =
        entries.computeIfPresent(envelopeId) { _, entry ->
            entry.copy(
                envelope = entry.envelope.copy(recipientDeviceRoutingId = recipientDeviceRoutingId)
            )
        }

    override suspend fun markAttempt(
        envelopeId: String,
        nextAttemptAtEpochMilliseconds: Long
    ): OutboundEnvelopeEntry? =
        entries.computeIfPresent(envelopeId) { _, entry ->
            entry.copy(
                attempts = entry.attempts + 1,
                nextAttemptAtEpochMilliseconds = nextAttemptAtEpochMilliseconds
            )
        }

    override suspend fun markStored(envelopeId: String) {
        entries.computeIfPresent(envelopeId) { _, entry ->
            entry.copy(state = EnvelopeAcceptanceState.STORED_AT_DESTINATION)
        }
    }

    override suspend fun get(envelopeId: String): OutboundEnvelopeEntry? {
        purgeExpired()
        return entries[envelopeId]
    }

    override suspend fun pendingDue(
        nowEpochMilliseconds: Long,
        limit: Int
    ): List<OutboundEnvelopeEntry> {
        require(limit > 0)
        purgeExpired()
        return entries.values
            .asSequence()
            .filter { it.state == EnvelopeAcceptanceState.QUEUED_AT_GATEWAY }
            .filter { it.nextAttemptAtEpochMilliseconds <= nowEpochMilliseconds }
            .sortedWith(
                compareBy<OutboundEnvelopeEntry>(OutboundEnvelopeEntry::nextAttemptAtEpochMilliseconds)
                    .thenBy { it.envelope.envelopeId }
            ).take(limit)
            .toList()
    }

    override suspend fun pendingCount(): Int {
        purgeExpired()
        return entries.values.count { it.state == EnvelopeAcceptanceState.QUEUED_AT_GATEWAY }
    }

    override fun close() = Unit

    private fun purgeExpired() {
        val currentTime = now()
        entries.entries.removeIf { it.value.envelope.expiresAtEpochMilliseconds <= currentTime }
    }
}
