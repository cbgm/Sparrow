package com.cbgm.sparrow.feature.attachments.domain.repository

import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentContent
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentStorageSummary
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTarget
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTranscript
import com.cbgm.sparrow.feature.attachments.domain.model.LocalAttachment
import kotlinx.coroutines.flow.Flow

interface MessageAttachmentRepository {
    suspend fun loadContent(target: AttachmentTarget): Result<AttachmentContent>

    suspend fun loadBytes(attachmentId: String): Result<ByteArray>

    suspend fun loadBytes(target: AttachmentTarget): Result<ByteArray>

    suspend fun saveTranscript(attachmentId: String, transcription: AttachmentTranscript): Result<Unit>

    fun observeTranscript(attachmentId: String): Flow<AttachmentTranscript?>

    fun observeLocalAttachments(conversationId: String): Flow<List<LocalAttachment>>

    fun observeStorageSummaries(): Flow<List<AttachmentStorageSummary>>

    suspend fun deleteLocalAttachments(attachmentIds: Set<String>): Result<Unit>

    suspend fun deleteLocalAttachmentsForConversation(conversationId: String): Result<Unit>
}
