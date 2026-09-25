package com.cbgm.sparrow.feature.autoreply.domain.usecase

import com.cbgm.sparrow.feature.autoreply.domain.model.AutoReply
import com.cbgm.sparrow.feature.autoreply.domain.repository.AutoReplyRepository
import kotlinx.coroutines.flow.Flow

class ObserveAutoRepliesUseCase(
    private val repository: AutoReplyRepository
) {
    operator fun invoke(): Flow<List<AutoReply>> = repository.observeAll()
}
