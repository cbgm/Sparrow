package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.AutoReplyEntity
import com.cbgm.sparrow.data.database.entity.AutoReplyRecipientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoReplyDao {
    @Query(
        """
        SELECT *
        FROM auto_replies
        ORDER BY createdAtEpochMilliseconds ASC, id ASC
        """
    )
    fun observeAll(): Flow<List<AutoReplyEntity>>

    @Query(
        """
        SELECT *
        FROM auto_replies
        WHERE isActive = 1
        LIMIT 1
        """
    )
    fun observeActive(): Flow<AutoReplyEntity?>

    @Query("SELECT * FROM auto_replies WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AutoReplyEntity?

    @Query(
        """
        SELECT *
        FROM auto_replies
        WHERE isActive = 1
        LIMIT 1
        """
    )
    suspend fun getActive(): AutoReplyEntity?

    @Upsert
    suspend fun upsert(entity: AutoReplyEntity)

    @Query(
        """
        UPDATE auto_replies
        SET name = :name,
            text = :text,
            updatedAtEpochMilliseconds = :updatedAtEpochMilliseconds
        WHERE id = :id
        """
    )
    suspend fun updateContent(
        id: String,
        name: String,
        text: String,
        updatedAtEpochMilliseconds: Long
    ): Int

    @Query("DELETE FROM auto_replies WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("UPDATE auto_replies SET isActive = 0, activationSessionId = NULL WHERE isActive = 1")
    suspend fun deactivateAll()

    @Query("UPDATE auto_replies SET isActive = 1, activationSessionId = :activationSessionId WHERE id = :id")
    suspend fun setActive(
        id: String,
        activationSessionId: String
    ): Int

    @Query("DELETE FROM auto_reply_recipients")
    suspend fun clearRecipients()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecipient(entity: AutoReplyRecipientEntity): Long

    @Query("DELETE FROM auto_reply_recipients WHERE contactId = :contactId")
    suspend fun deleteRecipient(contactId: String)

    @Transaction
    suspend fun activate(
        id: String,
        activationSessionId: String
    ) {
        checkNotNull(getById(id)) { "Auto reply not found: $id" }
        deactivateAll()
        check(setActive(id, activationSessionId) == 1) { "Auto reply could not be activated: $id" }
        clearRecipients()
    }

    @Transaction
    suspend fun deactivate() {
        deactivateAll()
        clearRecipients()
    }

    @Transaction
    suspend fun delete(id: String) {
        val existing = getById(id) ?: return
        check(deleteById(id) == 1) { "Auto reply could not be deleted: $id" }
        if (existing.isActive) {
            clearRecipients()
        }
    }

    @Transaction
    suspend fun claimForContact(
        contactId: String,
        sentAtEpochMilliseconds: Long
    ): AutoReplyEntity? {
        val active = getActive() ?: return null
        val inserted =
            insertRecipient(
                AutoReplyRecipientEntity(
                    contactId = contactId,
                    sentAtEpochMilliseconds = sentAtEpochMilliseconds
                )
            )
        return active.takeIf { inserted != -1L }
    }

    @Transaction
    suspend fun releaseContactClaim(
        contactId: String,
        expectedActivationSessionId: String
    ) {
        val active = getActive() ?: return
        if (active.activationSessionId == expectedActivationSessionId) {
            deleteRecipient(contactId)
        }
    }
}
