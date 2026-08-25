package com.cbgm.sparrow.feature.attachments.domain.usecase

import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentStorageSummary
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import kotlinx.coroutines.flow.Flow

class ObserveAttachmentStorageSummariesUseCase(
    private val repository: MessageAttachmentRepository
) {
    operator fun invoke(): Flow<List<AttachmentStorageSummary>> = repository.observeStorageSummaries()
}
