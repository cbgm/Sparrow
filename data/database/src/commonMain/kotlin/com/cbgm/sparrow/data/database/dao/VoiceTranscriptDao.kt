package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.VoiceTranscriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceTranscriptDao {
    @Upsert
    suspend fun upsert(transcript: VoiceTranscriptEntity)

    @Query("SELECT transcript FROM voice_transcripts WHERE partId = :partId LIMIT 1")
    fun observe(partId: String): Flow<String?>
}
