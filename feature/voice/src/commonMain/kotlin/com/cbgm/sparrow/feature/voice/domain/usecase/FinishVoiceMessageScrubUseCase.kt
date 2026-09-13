package com.cbgm.sparrow.feature.voice.domain.usecase

import com.cbgm.sparrow.feature.attachments.domain.usecase.LoadAttachmentBytesUseCase
import com.cbgm.sparrow.feature.voice.domain.model.VoiceMessageTarget
import com.cbgm.sparrow.feature.voice.domain.repository.VoiceRepository

class FinishVoiceMessageScrubUseCase(
    private val loadAttachmentBytes: LoadAttachmentBytesUseCase,
    private val repository: VoiceRepository
) {
    suspend operator fun invoke(target: VoiceMessageTarget, positionMilliseconds: Long): Result<Unit> =
        loadAttachmentBytes(target.attachmentTarget)
            .mapCatching { bytes ->
                repository.finishMessageScrub(
                    attachmentId = target.attachmentId,
                    bytes = bytes,
                    durationMilliseconds = target.durationMilliseconds,
                    positionMilliseconds = positionMilliseconds
                ).getOrThrow()
            }
}
