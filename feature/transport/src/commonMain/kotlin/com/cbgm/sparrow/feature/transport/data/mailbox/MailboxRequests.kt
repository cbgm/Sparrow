package com.cbgm.sparrow.feature.transport.data.mailbox

import com.cbgm.sparrow.protocol.mailbox.MailboxDeliveryRoute
import kotlinx.serialization.Serializable

@Serializable
internal data class CreateMailboxRequestDto(
    val nodeId: String,
    val nodeEndpoint: String,
    val routeSequence: Long,
    val expiresAtEpochMilliseconds: Long,
    val creationWork: MailboxCreationWorkDto,
    val invitation: Boolean = false
)

/** The proof of work that pays for a new mailbox ([MailboxCreationWorkSolver]). */
@Serializable
internal data class MailboxCreationWorkDto(
    val stamp: String,
    val createdAtEpochMilliseconds: Long,
    val nonce: Long
)

@Serializable
internal data class MailboxEnvelopeRequestDto(
    val envelope: FederatedEnvelopeDto,
    /** Only an invitation mailbox asks for it. */
    val depositWork: MailboxCreationWorkDto? = null
)

@Serializable
internal data class MailboxWaitRequestDto(
    val mailboxes: List<MailboxWaitEntryDto>
)

@Serializable
internal data class MailboxWaitEntryDto(
    val mailboxId: String,
    val retrievalCapability: String
)

@Serializable
internal data class MailboxWaitResponseDto(
    val ready: List<String>
)

@Serializable
internal data class CreateMailboxResponseDto(
    val deliveryRoute: MailboxDeliveryRoute,
    val retrievalCapability: String
)
