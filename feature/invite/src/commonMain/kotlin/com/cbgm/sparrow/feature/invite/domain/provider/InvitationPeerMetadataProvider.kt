package com.cbgm.sparrow.feature.invite.domain.provider

import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType

data class InvitationPeerMetadata(
    val displayName: String? = null,
    val secondaryText: String? = null
)

interface InvitationPeerMetadataProvider {
    val payloadType: InvitationPayloadType

    suspend fun get(peerId: String): Result<InvitationPeerMetadata>
}
