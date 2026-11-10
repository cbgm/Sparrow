package com.cbgm.sparrow.server.push

import com.cbgm.sparrow.server.protocol.TransportEnvelope
import java.util.concurrent.ConcurrentHashMap

interface PendingEnvelopeStore {
    suspend fun enqueue(envelope: TransportEnvelope): Boolean

    suspend fun pending(recipientId: String): List<TransportEnvelope>

    suspend fun remove(
        recipientId: String,
        envelopeId: String
    )

    suspend fun contains(envelopeId: String): Boolean

    suspend fun pendingRecipientIds(): Set<String>

    suspend fun count(): Int
}

class InMemoryPendingEnvelopeStore(
    private val maximumEnvelopes: Int = DEFAULT_MAXIMUM_ENVELOPES
) : PendingEnvelopeStore {
    private val envelopes = ConcurrentHashMap<String, ConcurrentHashMap<String, TransportEnvelope>>()

    init {
        require(maximumEnvelopes > 0) {
            "Maximum envelope count must be positive"
        }
    }

    override suspend fun enqueue(envelope: TransportEnvelope): Boolean {
        if (count() >= maximumEnvelopes && !contains(envelope.envelopeId)) {
            return false
        }

        val recipientEnvelopes =
            envelopes.computeIfAbsent(envelope.recipientId) {
                ConcurrentHashMap()
            }

        return recipientEnvelopes.putIfAbsent(envelope.envelopeId, envelope) == null
    }

    override suspend fun pending(recipientId: String): List<TransportEnvelope> =
        envelopes[recipientId]
            ?.values
            ?.sortedBy(TransportEnvelope::createdAtEpochMilliseconds)
            .orEmpty()

    override suspend fun remove(
        recipientId: String,
        envelopeId: String
    ) {
        envelopes[recipientId]?.let { recipientEnvelopes ->
            recipientEnvelopes.remove(envelopeId)

            if (recipientEnvelopes.isEmpty()) {
                envelopes.remove(recipientId, recipientEnvelopes)
            }
        }
    }

    override suspend fun contains(envelopeId: String): Boolean = envelopes.values.any { it.containsKey(envelopeId) }

    override suspend fun pendingRecipientIds(): Set<String> =
        envelopes
            .filterValues { it.isNotEmpty() }
            .keys
            .toSet()

    override suspend fun count(): Int = envelopes.values.sumOf { it.size }

    private companion object {
        const val DEFAULT_MAXIMUM_ENVELOPES = 100_000
    }
}

/** Stores [envelope] unless it is there already; false only when there is no room for it. */
suspend fun PendingEnvelopeStore.store(envelope: TransportEnvelope): Boolean = enqueue(envelope) || contains(envelope.envelopeId)

class PushStores(
    val pendingEnvelopes: PendingEnvelopeStore,
    val persistenceMode: String,
    private val closeAction: () -> Unit = {}
) : AutoCloseable {
    override fun close() {
        closeAction()
    }

    companion object {
        fun inMemory(config: PushConfig): PushStores =
            PushStores(
                pendingEnvelopes =
                    InMemoryPendingEnvelopeStore(
                        maximumEnvelopes = config.maximumEnvelopes
                    ),
                persistenceMode = "memory"
            )
    }
}
