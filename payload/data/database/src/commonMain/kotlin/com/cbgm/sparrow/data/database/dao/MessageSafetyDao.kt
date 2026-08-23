package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.MessageSafetyAssessmentEntity
import com.cbgm.sparrow.data.database.model.MessageSafetySource
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageSafetyDao {
    @Query(
        """
        SELECT
            messages.id AS messageId,
            messages.text AS text
        FROM messages
        LEFT JOIN message_safety_assessments
            ON message_safety_assessments.messageId = messages.id
            AND message_safety_assessments.analyzerVersion = :analyzerVersion
        WHERE messages.isMine = 0
          AND messages.contentStatus = 'READABLE'
          AND TRIM(messages.text) != ''
          AND messages.transportMode NOT LIKE 'SYSTEM_%'
          AND message_safety_assessments.messageId IS NULL
        ORDER BY messages.createdAtEpochMilliseconds ASC
        LIMIT :limit
        """
    )
    suspend fun getMessagesMissingAssessment(
        analyzerVersion: Int,
        limit: Int
    ): List<MessageSafetySource>

    @Query(
        """
        SELECT COUNT(*)
        FROM messages
        LEFT JOIN message_safety_assessments
            ON message_safety_assessments.messageId = messages.id
            AND message_safety_assessments.analyzerVersion = :analyzerVersion
        WHERE messages.isMine = 0
          AND messages.contentStatus = 'READABLE'
          AND TRIM(messages.text) != ''
          AND messages.transportMode NOT LIKE 'SYSTEM_%'
          AND message_safety_assessments.messageId IS NULL
        """
    )
    suspend fun getUnassessedMessageCount(analyzerVersion: Int): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM messages
        LEFT JOIN message_safety_assessments
            ON message_safety_assessments.messageId = messages.id
            AND message_safety_assessments.analyzerVersion = :analyzerVersion
        WHERE messages.isMine = 0
          AND messages.contentStatus = 'READABLE'
          AND TRIM(messages.text) != ''
          AND messages.transportMode NOT LIKE 'SYSTEM_%'
          AND message_safety_assessments.messageId IS NULL
        """
    )
    fun observeUnassessedMessageCount(analyzerVersion: Int): Flow<Int>

    @Query(
        """
        SELECT *
        FROM message_safety_assessments
        WHERE analyzerVersion = :analyzerVersion
          AND TRIM(reasons) != ''
        """
    )
    fun observeVisibleAssessments(analyzerVersion: Int): Flow<List<MessageSafetyAssessmentEntity>>

    @Upsert
    suspend fun upsertAssessments(assessments: List<MessageSafetyAssessmentEntity>)

    @Query("DELETE FROM message_safety_assessments")
    suspend fun deleteAllAssessments()

    @Query("DELETE FROM message_safety_assessments WHERE analyzerVersion != :analyzerVersion")
    suspend fun deleteAssessmentsForOtherAnalyzers(analyzerVersion: Int)
}
