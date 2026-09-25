package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleStatus
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository
import kotlinx.coroutines.flow.Flow

class ObserveInvitationLifecycleStatusUseCase(
    private val repository: InvitationRepository
) {
    operator fun invoke(
        payloadType: InvitationPayloadType,
        payloadId: String,
        peerId: String,
        direction: InvitationDirection
    ): Flow<InvitationLifecycleStatus?> =
        repository.observeLifecycleStatus(
            payloadType = payloadType,
            payloadId = payloadId,
            peerId = peerId,
            direction = direction
        )
}
