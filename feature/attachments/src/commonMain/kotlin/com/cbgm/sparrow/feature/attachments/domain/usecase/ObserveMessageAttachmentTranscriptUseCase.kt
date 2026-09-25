package com.cbgm.sparrow.feature.attachments.domain.usecase

import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTranscript
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import kotlinx.coroutines.flow.Flow

class ObserveMessageAttachmentTranscriptUseCase(
    private val repository: MessageAttachmentRepository
) {
    operator fun invoke(attachmentId: String): Flow<AttachmentTranscript?> =
        repository.observeTranscript(attachmentId)
}
