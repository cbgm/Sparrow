package com.cbgm.sparrow.feature.invite.domain.model

data class InvitationResult(
    val invitationId: String,
    val payloadType: InvitationPayloadType,
    val payloadId: String,
    val peerId: String,
    val direction: InvitationDirection,
    val response: InvitationResponse,
    val action: InvitationResultAction? = null
)
