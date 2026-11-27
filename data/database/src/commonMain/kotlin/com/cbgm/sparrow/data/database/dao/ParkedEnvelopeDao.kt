package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbgm.sparrow.data.database.entity.ParkedEnvelopeEntity

@Dao
interface ParkedEnvelopeDao {
    /** Returns -1 when the envelope is already parked. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(envelope: ParkedEnvelopeEntity): Long

    @Query("UPDATE parked_envelopes SET waitingKey = :waitingKey WHERE envelopeId = :envelopeId")
    suspend fun updateWaitingKey(envelopeId: String, waitingKey: String)

    /** A parked envelope keeps its place in the arrival order and only waits for something else. */
    @Transaction
    suspend fun park(envelope: ParkedEnvelopeEntity) {
        if (insert(envelope) == -1L) updateWaitingKey(envelope.envelopeId, envelope.waitingKey)
    }

    @Query("SELECT DISTINCT waitingKey FROM parked_envelopes")
    suspend fun findWaitingKeys(): List<String>

    @Query(
        "SELECT * FROM parked_envelopes WHERE waitingKey IN (:waitingKeys) " +
            "ORDER BY parkedAtEpochMilliseconds, id"
    )
    suspend fun findByWaitingKeys(waitingKeys: List<String>): List<ParkedEnvelopeEntity>

    @Query("DELETE FROM parked_envelopes WHERE envelopeId = :envelopeId")
    suspend fun delete(envelopeId: String)

    @Query("DELETE FROM parked_envelopes WHERE parkedAtEpochMilliseconds < :cutoffEpochMilliseconds")
    suspend fun deleteParkedBefore(cutoffEpochMilliseconds: Long)
}
