package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.core.messagepart.data.mapper.toMessagePart
import com.cbgm.sparrow.core.messagepart.data.model.MessagePartDto
import com.cbgm.sparrow.core.messagepart.data.model.PollDto
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupPinDataSource
import com.cbgm.sparrow.feature.chats.data.group.mapper.toDomain
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupPin
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupPinReadRepository
import com.cbgm.sparrow.protocol.attachment.GroupPinnedAttachmentProvider
import com.cbgm.sparrow.protocol.message.GroupMessageContentCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class GroupPinReadRepositoryImpl(
    private val attachmentDataSource: MessageAttachmentOperationsRepository,
    private val dataSource: GroupPinDataSource,
    private val groupMessageContentCodec: GroupMessageContentCodec
) : GroupPinReadRepository,
    GroupPinnedAttachmentProvider {
    override fun observe(groupId: String): Flow<GroupPin?> =
        dataSource
            .observe(groupId)
            .map { entity -> entity?.toDomain(groupMessageContentCodec) }

    override suspend fun load(groupId: String, attachmentId: String): ByteArray =
        loadAttachment(groupId, attachmentId).getOrThrow()

    override suspend fun loadAttachment(
        groupId: String,
        attachmentId: String
    ): Result<ByteArray> =
        safeSuspendCall {
            attachmentDataSource.loadDetachedBytes(
                findPinnedAttachment(groupId, attachmentId).toMessagePart()
            ).getOrThrow()
        }

    private suspend fun findPinnedAttachment(
        groupId: String,
        attachmentId: String
    ): MessagePartDto {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(attachmentId.isNotBlank()) { "Attachment ID must not be blank" }

        val state = dataSource.get(groupId) ?: error("Group pin was not found")
        val encodedContent = state.messageContent ?: error("Group pin has no message content")
        val content = groupMessageContentCodec.decode(encodedContent)
        return content.parts.firstOrNull { part -> part.id == attachmentId }
            ?: content.parts
                .filterIsInstance<PollDto>()
                .flatMap(PollDto::images)
                .firstOrNull { image -> image.id == attachmentId }
            ?: error("Pinned message attachment was not found")
    }
}
