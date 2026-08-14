package com.cbgm.securechat.server.mailbox

import com.cbgm.securechat.server.protocol.CreateMailboxRequest
import com.cbgm.securechat.server.protocol.CreateMailboxResponse
import com.cbgm.securechat.server.protocol.DeliveryRoute
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

interface MailboxStorage : AutoCloseable {
    val persistenceMode: String

    suspend fun create(request: CreateMailboxRequest): CreateMailboxResponse =
        when (
            val result =
                createWithQuota(
                    request = request,
                    ownerKeyHash = UNATTRIBUTED_OWNER,
                    maximumMailboxes = Int.MAX_VALUE,
                    maximumMailboxesPerOwner = Int.MAX_VALUE
                )
        ) {
            is MailboxCreationResult.Created -> result.response
            MailboxCreationResult.GlobalQuotaExceeded,
            MailboxCreationResult.OwnerQuotaExceeded ->
                error("Unlimited mailbox creation was rejected")
        }

    suspend fun createWithQuota(
        request: CreateMailboxRequest,
        ownerKeyHash: String,
        maximumMailboxes: Int,
        maximumMailboxesPerOwner: Int
    ): MailboxCreationResult

    suspend fun store(
        mailboxId: String,
        sendCapability: String,
        envelope: FederatedEnvelope
    ): MailboxResult

    suspend fun pending(
        mailboxId: String,
        retrievalCapability: String
    ): List<FederatedEnvelope>?

    suspend fun acknowledge(
        mailboxId: String,
        retrievalCapability: String,
        envelopeId: String
    ): Boolean

    suspend fun revoke(
        mailboxId: String,
        retrievalCapability: String
    ): MailboxRevocationResult

    suspend fun mailboxCount(): Int
}

class MailboxStore(
    private val maximumEnvelopeBytes: Int = DEFAULT_MAXIMUM_ENVELOPE_BYTES,
    private val maximumMailboxBytes: Long = DEFAULT_MAXIMUM_MAILBOX_BYTES,
    private val now: () -> Long = System::currentTimeMillis
) : MailboxStorage {
    private val mailboxes = ConcurrentHashMap<String, InMemoryMailbox>()
    private val secureRandom = SecureRandom()
    private val creationLock = Any()

    override val persistenceMode: String = "memory"

    override suspend fun createWithQuota(
        request: CreateMailboxRequest,
        ownerKeyHash: String,
        maximumMailboxes: Int,
        maximumMailboxesPerOwner: Int
    ): MailboxCreationResult =
        synchronized(creationLock) {
            require(request.expiresAtEpochMilliseconds > now())
            validateCreation(ownerKeyHash, maximumMailboxes, maximumMailboxesPerOwner)
            purgeExpiredMailboxes(mailboxes, now())
            if (mailboxes.size >= maximumMailboxes) {
                return@synchronized MailboxCreationResult.GlobalQuotaExceeded
            }
            if (
                mailboxes.values.count { it.ownerKeyHash == ownerKeyHash } >=
                maximumMailboxesPerOwner
            ) {
                return@synchronized MailboxCreationResult.OwnerQuotaExceeded
            }

            val mailboxId = randomToken(secureRandom)
            val sendCapability = randomToken(secureRandom)
            val retrievalCapability = randomToken(secureRandom)
            mailboxes[mailboxId] =
                InMemoryMailbox(
                    ownerKeyHash = ownerKeyHash,
                    sendCapabilityHash = hashCapability(sendCapability),
                    retrievalCapabilityHash = hashCapability(retrievalCapability),
                    expiresAtEpochMilliseconds = request.expiresAtEpochMilliseconds
                )

            MailboxCreationResult.Created(
                CreateMailboxResponse(
                    deliveryRoute =
                        DeliveryRoute(
                            routeId = randomToken(secureRandom),
                            nodeId = request.nodeId,
                            nodeEndpoint = request.nodeEndpoint,
                            mailboxId = mailboxId,
                            sendCapability = sendCapability,
                            sequence = request.routeSequence,
                            expiresAtEpochMilliseconds = request.expiresAtEpochMilliseconds,
                            identitySignature = byteArrayOf()
                        ),
                    retrievalCapability = retrievalCapability
                )
            )
        }

    override suspend fun store(
        mailboxId: String,
        sendCapability: String,
        envelope: FederatedEnvelope
    ): MailboxResult {
        val mailbox = activeMailbox(mailboxId)
        return when {
            mailbox == null -> MailboxResult.Rejected("MAILBOX_NOT_FOUND")
            !capabilityMatches(sendCapability, mailbox.sendCapabilityHash) ->
                MailboxResult.Rejected("INVALID_CAPABILITY")

            envelope.expiresAtEpochMilliseconds <= now() ->
                MailboxResult.Rejected("ENVELOPE_EXPIRED")

            envelope.encryptedPayload.encodeToByteArray().size > maximumEnvelopeBytes ->
                MailboxResult.Rejected("ENVELOPE_TOO_LARGE")

            envelope.mailboxRoute?.mailboxId != mailboxId ->
                MailboxResult.Rejected("MAILBOX_ROUTE_MISMATCH")

            else -> storeValidatedEnvelope(mailbox, envelope)
        }
    }

    override suspend fun pending(
        mailboxId: String,
        retrievalCapability: String
    ): List<FederatedEnvelope>? {
        val mailbox = activeMailbox(mailboxId)
        return if (
            mailbox != null &&
            capabilityMatches(retrievalCapability, mailbox.retrievalCapabilityHash)
        ) {
            purgeExpiredEnvelopes(mailbox, now())
            mailbox.envelopes.values.sortedBy(FederatedEnvelope::createdAtEpochMilliseconds)
        } else {
            null
        }
    }

    override suspend fun acknowledge(
        mailboxId: String,
        retrievalCapability: String,
        envelopeId: String
    ): Boolean {
        val mailbox = activeMailbox(mailboxId)
        return if (
            mailbox != null &&
            capabilityMatches(retrievalCapability, mailbox.retrievalCapabilityHash)
        ) {
            mailbox.envelopes.remove(envelopeId)
            true
        } else {
            false
        }
    }

    override suspend fun revoke(
        mailboxId: String,
        retrievalCapability: String
    ): MailboxRevocationResult {
        val mailbox = activeMailbox(mailboxId)
        return when {
            mailbox == null -> MailboxRevocationResult.NotFound
            !capabilityMatches(retrievalCapability, mailbox.retrievalCapabilityHash) ->
                MailboxRevocationResult.Unauthorized

            else -> {
                mailboxes.remove(mailboxId, mailbox)
                MailboxRevocationResult.Revoked
            }
        }
    }

    override suspend fun mailboxCount(): Int {
        purgeExpiredMailboxes(mailboxes, now())
        return mailboxes.size
    }

    override fun close() = Unit

    private fun activeMailbox(mailboxId: String): InMemoryMailbox? {
        val mailbox = mailboxes[mailboxId]
        return if (mailbox != null && mailbox.expiresAtEpochMilliseconds <= now()) {
            mailboxes.remove(mailboxId, mailbox)
            null
        } else {
            mailbox
        }
    }

    private fun storeValidatedEnvelope(
        mailbox: InMemoryMailbox,
        envelope: FederatedEnvelope
    ): MailboxResult {
        purgeExpiredEnvelopes(mailbox, now())
        val duplicate = mailbox.envelopes.containsKey(envelope.envelopeId)
        val projectedBytes = mailbox.storedPayloadBytes() + envelope.payloadBytes()
        return if (!duplicate && projectedBytes > maximumMailboxBytes) {
            MailboxResult.Rejected("MAILBOX_QUOTA_EXCEEDED")
        } else {
            mailbox.envelopes.putIfAbsent(envelope.envelopeId, envelope)
            MailboxResult.Stored(duplicate)
        }
    }
}

private data class InMemoryMailbox(
    val ownerKeyHash: String,
    val sendCapabilityHash: ByteArray,
    val retrievalCapabilityHash: ByteArray,
    val expiresAtEpochMilliseconds: Long,
    val envelopes: ConcurrentHashMap<String, FederatedEnvelope> = ConcurrentHashMap()
)

sealed interface MailboxResult {
    data class Stored(
        val duplicate: Boolean
    ) : MailboxResult

    data class Rejected(
        val code: String
    ) : MailboxResult
}

sealed interface MailboxRevocationResult {
    data object Revoked : MailboxRevocationResult

    data object NotFound : MailboxRevocationResult

    data object Unauthorized : MailboxRevocationResult
}

sealed interface MailboxCreationResult {
    data class Created(
        val response: CreateMailboxResponse
    ) : MailboxCreationResult

    data object GlobalQuotaExceeded : MailboxCreationResult

    data object OwnerQuotaExceeded : MailboxCreationResult
}

private fun validateCreation(
    ownerKeyHash: String,
    maximumMailboxes: Int,
    maximumMailboxesPerOwner: Int
) {
    require(ownerKeyHash.isNotBlank()) { "Mailbox owner key must not be blank" }
    require(maximumMailboxes > 0) { "Maximum mailbox count must be positive" }
    require(maximumMailboxesPerOwner > 0) { "Per-owner mailbox count must be positive" }
}

private fun purgeExpiredEnvelopes(
    mailbox: InMemoryMailbox,
    currentTime: Long
) {
    mailbox.envelopes.entries.removeIf { (_, envelope) ->
        envelope.expiresAtEpochMilliseconds <= currentTime
    }
}

private fun purgeExpiredMailboxes(
    mailboxes: ConcurrentHashMap<String, InMemoryMailbox>,
    currentTime: Long
) {
    mailboxes.entries.removeIf { (_, mailbox) ->
        mailbox.expiresAtEpochMilliseconds <= currentTime
    }
}

private fun InMemoryMailbox.storedPayloadBytes(): Long =
    envelopes.values.sumOf(FederatedEnvelope::payloadBytes)

private fun FederatedEnvelope.payloadBytes(): Long =
    encryptedPayload.encodeToByteArray().size.toLong()

private fun randomToken(secureRandom: SecureRandom): String {
    val bytes = ByteArray(CAPABILITY_TOKEN_BYTES)
    secureRandom.nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

private fun hashCapability(value: String): ByteArray =
    MessageDigest.getInstance(CAPABILITY_HASH_ALGORITHM).digest(value.encodeToByteArray())

private fun capabilityMatches(
    capability: String,
    expectedHash: ByteArray
): Boolean = MessageDigest.isEqual(hashCapability(capability), expectedHash)

private const val UNATTRIBUTED_OWNER = "unattributed"
private const val DEFAULT_MAXIMUM_ENVELOPE_BYTES = 1_048_576
private const val DEFAULT_MAXIMUM_MAILBOX_BYTES = 100L * DEFAULT_MAXIMUM_ENVELOPE_BYTES
private const val CAPABILITY_TOKEN_BYTES = 32
private const val CAPABILITY_HASH_ALGORITHM = "SHA-256"
