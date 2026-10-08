package com.cbgm.sparrow.feature.voice.domain.usecase

import com.cbgm.sparrow.feature.attachments.domain.usecase.LoadAttachmentBytesUseCase
import com.cbgm.sparrow.feature.voice.domain.repository.VoiceRepository

class FinishVoiceMessageScrubUseCase(
    private val loadAttachmentBytes: LoadAttachmentBytesUseCase,
    private val repository: VoiceRepository
) {
    suspend operator fun invoke(
        partId: String,
        durationMilliseconds: Long,
        positionMilliseconds: Long,
        groupId: String? = null
    ): Result<Unit> =
        loadAttachmentBytes(partId, groupId)
            .mapCatching { bytes ->
                repository.finishMessageScrub(
                    attachmentId = partId,
                    bytes = bytes,
                    durationMilliseconds = durationMilliseconds,
                    positionMilliseconds = positionMilliseconds
                ).getOrThrow()
            }
}
