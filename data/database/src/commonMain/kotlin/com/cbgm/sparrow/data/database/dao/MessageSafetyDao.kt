package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.MessageSafetyAssessmentEntity
import com.cbgm.sparrow.data.database.model.MessageSafetySourceDto
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageSafetyDao {
    @Query(
        """
        SELECT
            messages.id AS messageId,
            message_text.text AS text
        FROM messages
        INNER JOIN message_parts
            ON message_parts.messageId = messages.id
            AND message_parts.type = 'TEXT'
        INNER JOIN message_text ON message_text.partId = message_parts.id
        LEFT JOIN message_safety_assessments
            ON message_safety_assessments.messageId = messages.id
            AND message_safety_assessments.analyzerVersion = :analyzerVersion
        WHERE messages.isMine = 0
          AND messages.contentStatus = 'READABLE'
          AND TRIM(message_text.text) != ''
          AND messages.transportMode NOT LIKE 'SYSTEM_%'
          AND message_safety_assessments.messageId IS NULL
        ORDER BY messages.createdAtEpochMilliseconds ASC
        LIMIT :limit
        """
    )
    suspend fun getMessagesMissingAssessment(
        analyzerVersion: Int,
        limit: Int
    ): List<MessageSafetySourceDto>

    @Query(
        """
        SELECT COUNT(*)
        FROM messages
        INNER JOIN message_parts
            ON message_parts.messageId = messages.id
            AND message_parts.type = 'TEXT'
        INNER JOIN message_text ON message_text.partId = message_parts.id
        LEFT JOIN message_safety_assessments
            ON message_safety_assessments.messageId = messages.id
            AND message_safety_assessments.analyzerVersion = :analyzerVersion
        WHERE messages.isMine = 0
          AND messages.contentStatus = 'READABLE'
          AND TRIM(message_text.text) != ''
          AND messages.transportMode NOT LIKE 'SYSTEM_%'
          AND message_safety_assessments.messageId IS NULL
        """
    )
    suspend fun getUnassessedMessageCount(analyzerVersion: Int): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM messages
        INNER JOIN message_parts
            ON message_parts.messageId = messages.id
            AND message_parts.type = 'TEXT'
        INNER JOIN message_text ON message_text.partId = message_parts.id
        LEFT JOIN message_safety_assessments
            ON message_safety_assessments.messageId = messages.id
            AND message_safety_assessments.analyzerVersion = :analyzerVersion
        WHERE messages.isMine = 0
          AND messages.contentStatus = 'READABLE'
          AND TRIM(message_text.text) != ''
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
