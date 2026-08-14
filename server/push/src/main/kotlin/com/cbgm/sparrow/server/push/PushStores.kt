package com.cbgm.sparrow.server.push

import com.cbgm.sparrow.server.protocol.TransportEnvelope
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

data class PushDevice(
    val routingId: String,
    val token: String,
    val platform: String
)

interface PushDeviceStore {
    suspend fun register(device: PushDevice)

    suspend fun find(routingId: String): List<PushDevice>

    suspend fun removeToken(token: String)

    suspend fun count(): Int
}

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

interface WakeUpStore {
    suspend fun create(recipientId: String): String

    suspend fun resolve(wakeUpId: String?): String?
}

class InMemoryPushDeviceStore : PushDeviceStore {
    private val devices = ConcurrentHashMap<String, ConcurrentHashMap<String, PushDevice>>()

    override suspend fun register(device: PushDevice) {
        devices.values.forEach { it.remove(device.token) }
        devices.computeIfAbsent(device.routingId) { ConcurrentHashMap() }[device.token] = device
    }

    override suspend fun find(routingId: String): List<PushDevice> = devices[routingId]?.values?.toList().orEmpty()

    override suspend fun removeToken(token: String) {
        devices.values.forEach { it.remove(token) }
    }

    override suspend fun count(): Int = devices.values.sumOf { it.size }
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

class InMemoryWakeUpStore(
    private val lifetimeMilliseconds: Long = DEFAULT_LIFETIME_MILLISECONDS,
    private val now: () -> Long = System::currentTimeMillis
) : WakeUpStore {
    private data class Entry(
        val recipientId: String,
        val expiresAtEpochMilliseconds: Long
    )

    private val entries = ConcurrentHashMap<String, Entry>()

    private val random = SecureRandom()

    init {
        require(lifetimeMilliseconds > 0L) {
            "Wake-up lifetime must be positive"
        }
    }

    override suspend fun create(recipientId: String): String {
        purgeExpired()

        val bytes = ByteArray(WAKE_UP_ID_BYTE_COUNT)
        random.nextBytes(bytes)

        val id =
            Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes)

        entries[id] =
            Entry(
                recipientId = recipientId,
                expiresAtEpochMilliseconds = now() + lifetimeMilliseconds
            )

        return id
    }

    override suspend fun resolve(wakeUpId: String?): String? {
        if (wakeUpId.isNullOrBlank()) {
            return null
        }

        purgeExpired()

        return entries[wakeUpId]?.recipientId
    }

    private fun purgeExpired() {
        val currentTime = now()

        entries.entries.removeIf { (_, entry) ->
            entry.expiresAtEpochMilliseconds <= currentTime
        }
    }

    private companion object {
        const val WAKE_UP_ID_BYTE_COUNT = 32
        const val DEFAULT_LIFETIME_MILLISECONDS = 15L * 60L * 1_000L
    }
}

class PushStores(
    val devices: PushDeviceStore,
    val pendingEnvelopes: PendingEnvelopeStore,
    val wakeUps: WakeUpStore,
    val persistenceMode: String,
    private val closeAction: () -> Unit = {}
) : AutoCloseable {
    override fun close() {
        closeAction()
    }

    companion object {
        fun inMemory(config: PushConfig): PushStores =
            PushStores(
                devices = InMemoryPushDeviceStore(),
                pendingEnvelopes =
                    InMemoryPendingEnvelopeStore(
                        maximumEnvelopes = config.maximumEnvelopes
                    ),
                wakeUps =
                    InMemoryWakeUpStore(
                        lifetimeMilliseconds = config.wakeUpLifetimeMilliseconds
                    ),
                persistenceMode = "memory"
            )
    }
}
