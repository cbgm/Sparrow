package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.usecase.SendInvitationUseCase

class AddGroupMembersUseCase(
    private val sendInvitation: SendInvitationUseCase
) {
    suspend operator fun invoke(
        groupId: String,
        contactIds: Set<String>
    ): Result<Unit> =
        sendInvitation(
            payloadType = InvitationPayloadType.GROUP,
            payloadId = groupId,
            peerIds = contactIds
        )
}
