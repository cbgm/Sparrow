package com.cbgm.sparrow.feature.chats.presentation.common.controller

import com.cbgm.sparrow.core.messagepart.domain.model.Voice
import com.cbgm.sparrow.feature.voice.domain.usecase.GetRecordedVoiceAttachmentUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.ResetVoiceComposerUseCase

class ConversationVoiceController(
    private val getRecordedVoiceAttachment: GetRecordedVoiceAttachmentUseCase,
    private val resetVoiceComposer: ResetVoiceComposerUseCase,
    private val media: ConversationMediaController
) {
    suspend fun recordedPart(): Result<Voice> = getRecordedVoiceAttachment()

    fun finishSent(parts: List<com.cbgm.sparrow.core.messagepart.domain.model.MessagePart>) {
        parts.filterIsInstance<Voice>().singleOrNull()?.localFilePath?.let {
            media.deleteLocalPaths(
                listOf(it)
            )
        }
        resetVoiceComposer()
    }
}
