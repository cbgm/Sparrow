package com.cbgm.sparrow.protocol.mailbox

import kotlinx.serialization.Serializable

@Serializable
data class MailboxDeliveryRoute(
    val routeId: String,
    val nodeId: String,
    val nodeEndpoint: String,
    val mailboxId: String,
    val sendCapability: String,
    val sequence: Long,
    val expiresAtEpochMilliseconds: Long,
    val identitySignature: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MailboxDeliveryRoute) return false

        return routeId == other.routeId &&
            nodeId == other.nodeId &&
            nodeEndpoint == other.nodeEndpoint &&
            mailboxId == other.mailboxId &&
            sendCapability == other.sendCapability &&
            sequence == other.sequence &&
            expiresAtEpochMilliseconds == other.expiresAtEpochMilliseconds &&
            identitySignature.contentEquals(other.identitySignature)
    }

    override fun hashCode(): Int {
        var result = routeId.hashCode()
        result = 31 * result + nodeId.hashCode()
        result = 31 * result + nodeEndpoint.hashCode()
        result = 31 * result + mailboxId.hashCode()
        result = 31 * result + sendCapability.hashCode()
        result = 31 * result + sequence.hashCode()
        result = 31 * result + expiresAtEpochMilliseconds.hashCode()
        result = 31 * result + identitySignature.contentHashCode()
        return result
    }
}

data class LocalMailboxCredential(
    val contactId: String,
    val deliveryRoute: MailboxDeliveryRoute,
    val accessEndpoint: String,
    val retrievalCapability: String,
    val revocationPending: Boolean = false
)

interface MailboxCapabilityLifecycle {
    suspend fun revokeForContact(contactId: String): Result<Unit>

    suspend fun revokeAll(): Result<Unit>

    suspend fun retryPendingRevocations(): Result<Int>
}

object NoOpMailboxCapabilityLifecycle : MailboxCapabilityLifecycle {
    override suspend fun revokeForContact(contactId: String): Result<Unit> = Result.success(Unit)

    override suspend fun revokeAll(): Result<Unit> = Result.success(Unit)

    override suspend fun retryPendingRevocations(): Result<Int> = Result.success(0)
}

interface MailboxRouteRepository {
    suspend fun localForContact(contactId: String): Result<LocalMailboxCredential?>

    suspend fun remoteForRecipientRoutingId(routingId: String): Result<MailboxDeliveryRoute?>

    suspend fun allLocal(): Result<List<LocalMailboxCredential>>

    suspend fun saveLocal(credential: LocalMailboxCredential): Result<Unit>

    suspend fun saveRemote(
        contactId: String,
        route: MailboxDeliveryRoute
    ): Result<Unit>

    suspend fun markLocalRevocationPending(contactId: String): Result<Unit>

    suspend fun deleteLocal(contactId: String): Result<Unit>

    suspend fun deleteRemote(contactId: String): Result<Unit>

    suspend fun deleteAllRemote(): Result<Unit>
}
