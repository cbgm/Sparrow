package com.cbgm.sparrow.feature.autoreply.domain.usecase

import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.autoreply.domain.model.AutoReply
import com.cbgm.sparrow.feature.autoreply.domain.repository.AutoReplyRepository

class ClaimAutoReplyForContactUseCase(
    private val repository: AutoReplyRepository
) {
    suspend operator fun invoke(contactId: String): Result<AutoReply?> =
        repository.claimForContact(
            contactId = contactId,
            sentAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
        )
}
