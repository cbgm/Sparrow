package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository

class HandleInvitationResponseUseCase(
    private val repository: InvitationRepository
) {
    suspend operator fun invoke(
        payloadType: InvitationPayloadType,
        invitationId: String,
        response: InvitationResponse,
        applyResponseEffects: suspend () -> Result<Unit>
    ): Result<Unit> {
        val effectsResult = applyResponseEffects()
        if (effectsResult.isFailure) {
            return effectsResult
        }

        return repository.applyResponse(
            payloadType = payloadType,
            invitationId = invitationId,
            response = response
        )
    }
}
