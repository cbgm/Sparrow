package com.cbgm.sparrow.feature.voice.data.datasource

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import kotlinx.coroutines.flow.Flow

class VoiceTranscriptionSettingsDataSource(
    private val dataStore: SparrowDataStore
) {
    fun observeEnabled(): Flow<Boolean> =
        dataStore.observeBoolean(
            key = KEY_VOICE_TRANSCRIPTION_ENABLED,
            defaultValue = false
        )

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit {
            putBoolean(KEY_VOICE_TRANSCRIPTION_ENABLED, enabled)
        }
    }

    private companion object {
        const val KEY_VOICE_TRANSCRIPTION_ENABLED = "media.voice_transcription_enabled"
    }
}
