package com.cbgm.sparrow.feature.transport.data.mailbox

import com.cbgm.sparrow.protocol.mailbox.MailboxDeliveryRoute
import kotlinx.serialization.Serializable

@Serializable
internal data class CreateMailboxRequest(
    val nodeId: String,
    val nodeEndpoint: String,
    val routeSequence: Long,
    val expiresAtEpochMilliseconds: Long,
    val creationWork: MailboxCreationWork,
    val invitation: Boolean = false
)

/** The proof of work that pays for a new mailbox ([MailboxCreationWorkSolver]). */
@Serializable
internal data class MailboxCreationWork(
    val stamp: String,
    val createdAtEpochMilliseconds: Long,
    val nonce: Long
)

@Serializable
internal data class MailboxEnvelopeRequest(
    val envelope: FederatedEnvelopeDto,
    /** Only an invitation mailbox asks for it. */
    val depositWork: MailboxCreationWork? = null
)

@Serializable
internal data class MailboxWaitRequest(
    val mailboxes: List<MailboxWaitEntry>
)

@Serializable
internal data class MailboxWaitEntry(
    val mailboxId: String,
    val retrievalCapability: String
)

@Serializable
internal data class MailboxWaitResponse(
    val ready: List<String>
)

@Serializable
internal data class CreateMailboxResponse(
    val deliveryRoute: MailboxDeliveryRoute,
    val retrievalCapability: String
)
