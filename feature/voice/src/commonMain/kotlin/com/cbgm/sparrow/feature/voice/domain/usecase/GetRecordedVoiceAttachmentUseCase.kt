package com.cbgm.sparrow.feature.voice.domain.usecase

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.voice.domain.repository.VoiceRepository

class GetRecordedVoiceAttachmentUseCase(
    private val repository: VoiceRepository
) {
    private val logger = SparrowLog.withTag("VoiceRecording")

    operator fun invoke(): Result<OutgoingMessageAttachment> {
        val recording = repository.currentRecording()
        if (recording == null) {
            val error = IllegalStateException("No recorded voice message is available")
            logger.error(error) { "Could not create recorded voice attachment" }
            return Result.failure(error)
        }

        return Result.success(
            OutgoingMessageAttachment(
                id = IdGenerator.generate(prefix = "voice"),
                type = MessageAttachmentType.VOICE,
                bytes = recording.bytes,
                mimeType = recording.mimeType,
                durationMilliseconds = recording.durationMilliseconds
            )
        )
    }
}
