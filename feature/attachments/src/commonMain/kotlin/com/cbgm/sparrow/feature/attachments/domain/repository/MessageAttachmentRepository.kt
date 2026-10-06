package com.cbgm.sparrow.feature.attachments.domain.repository

import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentStorageSummary
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTranscript
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import kotlinx.coroutines.flow.Flow

interface MessageAttachmentRepository {
    suspend fun loadLocalFile(partId: String, groupId: String? = null): Result<String>

    suspend fun loadLocation(partId: String, groupId: String? = null): Result<CurrentLocation>

    suspend fun loadContact(partId: String, groupId: String? = null): Result<SharedContact>

    suspend fun loadBytes(partId: String, groupId: String? = null): Result<ByteArray>

    suspend fun saveTranscript(attachmentId: String, transcription: AttachmentTranscript): Result<Unit>

    fun observeTranscript(attachmentId: String): Flow<AttachmentTranscript?>

    fun observeLocalAttachments(conversationId: String): Flow<List<MessagePart>>

    fun observeStorageSummaries(): Flow<List<AttachmentStorageSummary>>

    suspend fun deleteLocalAttachments(attachmentIds: Set<String>): Result<Unit>

    suspend fun deleteLocalAttachmentsForConversation(conversationId: String): Result<Unit>
}
