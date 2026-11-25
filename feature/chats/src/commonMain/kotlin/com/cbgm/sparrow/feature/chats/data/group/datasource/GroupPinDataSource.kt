package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.core.base.messagepart.data.model.PollDto
import com.cbgm.sparrow.data.database.dao.GroupPinDao
import com.cbgm.sparrow.data.database.entity.GroupPinEntity
import com.cbgm.sparrow.protocol.attachment.GroupPinnedAttachmentSource
import com.cbgm.sparrow.protocol.message.GroupMessageContentCodec
import kotlinx.coroutines.flow.Flow

internal class GroupPinDataSource(
    private val groupPinDao: GroupPinDao,
    private val groupMessageContentCodec: GroupMessageContentCodec
) : GroupPinnedAttachmentSource {
    fun observe(groupId: String): Flow<GroupPinEntity?> = groupPinDao.observe(groupId)

    suspend fun get(groupId: String): GroupPinEntity? = groupPinDao.get(groupId)

    override suspend fun isPinned(
        groupId: String,
        attachmentId: String
    ): Boolean {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(attachmentId.isNotBlank()) { "Attachment ID must not be blank" }

        val encodedContent = get(groupId)?.messageContent ?: return false
        val content = groupMessageContentCodec.decode(encodedContent)
        return content.parts.any { part -> part.id == attachmentId } ||
            content.parts.filterIsInstance<PollDto>().flatMap(PollDto::images).any { image -> image.id == attachmentId }
    }
}
