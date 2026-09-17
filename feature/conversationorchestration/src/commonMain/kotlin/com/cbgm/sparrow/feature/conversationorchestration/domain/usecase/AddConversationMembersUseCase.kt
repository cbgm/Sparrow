package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.usecase.SendInvitationUseCase

class AddConversationMembersUseCase(
    private val sendInvitation: SendInvitationUseCase
) {
    suspend operator fun invoke(
        conversationId: String,
        peerIds: Set<String>
    ): Result<Unit> =
        sendInvitation(
            payloadType = InvitationPayloadType.GROUP,
            payloadId = conversationId,
            peerIds = peerIds
        )
}
