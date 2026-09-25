package com.cbgm.sparrow.feature.voice.data.repository

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.voice.data.datasource.VoiceTranscriptionSettingsDataSource
import com.cbgm.sparrow.feature.voice.domain.repository.VoiceTranscriptionSettingsRepository
import kotlinx.coroutines.flow.Flow

class VoiceTranscriptionSettingsRepositoryImpl(
    private val dataSource: VoiceTranscriptionSettingsDataSource
) : VoiceTranscriptionSettingsRepository {
    private val logger = SparrowLog.withTag("VoiceTranscriptionSettings")

    override fun observeEnabled(): Flow<Boolean> = dataSource.observeEnabled()

    override suspend fun setEnabled(enabled: Boolean): Result<Unit> =
        safeSuspendCall {
            dataSource.setEnabled(enabled)
        }.onFailure { error ->
            logger.error(error) { "Could not update voice transcription setting" }
        }
}
