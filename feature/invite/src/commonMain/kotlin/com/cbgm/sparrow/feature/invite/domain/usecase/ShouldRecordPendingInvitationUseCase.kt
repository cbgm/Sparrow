package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository

class ShouldRecordPendingInvitationUseCase(
    private val repository: InvitationRepository
) {
    suspend operator fun invoke(record: InvitationLifecycleRecord): Result<Boolean> =
        repository.shouldRecordPending(record)
}
