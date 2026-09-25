package com.cbgm.sparrow.feature.voice.domain.usecase

import com.cbgm.sparrow.feature.voice.domain.repository.VoiceRepository

class StartVoiceMessageScrubUseCase(
    private val repository: VoiceRepository
) {
    operator fun invoke(attachmentId: String) = repository.startMessageScrub(attachmentId)
}
