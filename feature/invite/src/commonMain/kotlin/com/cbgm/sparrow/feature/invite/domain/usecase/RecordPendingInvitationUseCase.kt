package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository

class RecordPendingInvitationUseCase(
    private val repository: InvitationRepository
) {
    suspend operator fun invoke(record: InvitationLifecycleRecord): Result<Unit> =
        repository.recordPending(record)
}
