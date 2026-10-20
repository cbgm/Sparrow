package com.cbgm.sparrow.feature.voice.domain.usecase

import com.cbgm.sparrow.core.base.messagepart.domain.model.Voice
import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.media.domain.repository.MediaSelectionFileRepository
import com.cbgm.sparrow.feature.voice.domain.repository.VoiceRepository

class GetRecordedVoiceAttachmentUseCase(
    private val repository: VoiceRepository,
    private val mediaFiles: MediaSelectionFileRepository
) {
    private val logger = SparrowLog.withTag("VoiceRecording")

    suspend operator fun invoke(): Result<Voice> =
        safeSuspendCall(logFailure = false) {
            val recording = repository.currentRecording().getOrThrow()
                ?: throw IllegalStateException("No recorded voice message is available")
            val id = IdGenerator.generate(prefix = "voice")
            val stored =
                mediaFiles.save(id = id, bytes = recording.bytes, previewBytes = null).getOrThrow()

            Voice(
                id = id,
                mimeType = recording.mimeType,
                byteSize = recording.bytes.size.toLong(),
                durationMilliseconds = recording.durationMilliseconds,
                localFilePath = stored.localFilePath
            )
        }.onFailure { error ->
            logger.error(error) { "Could not create recorded voice message part" }
        }
}
