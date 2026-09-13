package com.cbgm.sparrow.feature.voice.domain.repository

import com.cbgm.sparrow.feature.voice.domain.model.VoiceComposerState
import com.cbgm.sparrow.feature.voice.domain.model.VoicePlaybackState
import com.cbgm.sparrow.feature.voice.domain.model.VoiceRecording
import kotlinx.coroutines.flow.Flow

interface VoiceRepository {
    fun observeComposerState(): Flow<VoiceComposerState>

    fun observePlaybackState(attachmentId: String): Flow<VoicePlaybackState>

    suspend fun startRecording(): Result<Unit>

    suspend fun stopRecording(): Result<Unit>

    fun togglePreview(): Result<Unit>

    suspend fun cancelRecording()

    fun resetComposer()

    fun currentRecording(): VoiceRecording?

    fun toggleMessagePlayback(
        attachmentId: String,
        bytes: ByteArray,
        durationMilliseconds: Long
    ): Result<Unit>

    fun startMessageScrub(attachmentId: String)

    fun finishMessageScrub(
        attachmentId: String,
        bytes: ByteArray,
        durationMilliseconds: Long,
        positionMilliseconds: Long
    ): Result<Unit>
}
