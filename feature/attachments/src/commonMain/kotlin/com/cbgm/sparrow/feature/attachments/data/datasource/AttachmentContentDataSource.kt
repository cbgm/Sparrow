package com.cbgm.sparrow.feature.attachments.data.datasource

import com.cbgm.sparrow.core.protocol.attachment.GroupPinnedAttachmentProvider
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.data.model.AttachmentContentPayloadDto
import com.cbgm.sparrow.feature.attachments.data.model.AttachmentTargetDto

internal class AttachmentContentDataSource(
    private val messageAttachmentDataSource: MessageAttachmentDataSource,
    private val messageAttachmentFileDataSource: MessageAttachmentFileDataSource,
    private val groupPinnedAttachmentProvider: GroupPinnedAttachmentProvider
) {
    suspend fun load(target: AttachmentTargetDto): AttachmentContentPayloadDto =
        if (target.groupId == null) {
            loadMessageAttachment(target)
        } else {
            loadPinnedAttachment(target.groupId, target)
        }

    suspend fun loadBytes(target: AttachmentTargetDto): ByteArray =
        if (target.groupId == null) {
            messageAttachmentDataSource.loadBytes(target.id)
        } else {
            groupPinnedAttachmentProvider.load(target.groupId, target.id)
        }

    private suspend fun loadMessageAttachment(target: AttachmentTargetDto): AttachmentContentPayloadDto =
        when (target.type) {
            MessageAttachmentType.IMAGE,
            MessageAttachmentType.VIDEO,
            MessageAttachmentType.FILE -> {
                messageAttachmentDataSource.resolveLocalFilePath(target.id)?.let { localFilePath ->
                    return AttachmentContentPayloadDto.LocalFile(localFilePath)
                }

                messageAttachmentDataSource.loadBytes(target.id)
                val localFilePath =
                    requireNotNull(messageAttachmentDataSource.resolveLocalFilePath(target.id)) {
                        "Attachment was loaded but no local cache file exists"
                    }
                AttachmentContentPayloadDto.LocalFile(localFilePath)
            }

            MessageAttachmentType.LOCATION,
            MessageAttachmentType.CONTACT ->
                AttachmentContentPayloadDto.Payload(
                    messageAttachmentDataSource.loadBytes(target.id)
                )

            MessageAttachmentType.VOICE ->
                error("Voice attachments are loaded by the voice-message path")
        }

    private suspend fun loadPinnedAttachment(
        groupId: String,
        target: AttachmentTargetDto
    ): AttachmentContentPayloadDto {
        val bytes = groupPinnedAttachmentProvider.load(groupId, target.id)

        return when (target.type) {
            MessageAttachmentType.IMAGE,
            MessageAttachmentType.VIDEO,
            MessageAttachmentType.FILE -> {
                val fileName = messageAttachmentFileDataSource.write(bytes)
                val localFilePath =
                    requireNotNull(messageAttachmentFileDataSource.resolveCacheFilePath(fileName)) {
                        "Pinned attachment cache file could not be resolved"
                    }
                AttachmentContentPayloadDto.LocalFile(localFilePath)
            }

            MessageAttachmentType.LOCATION,
            MessageAttachmentType.CONTACT -> AttachmentContentPayloadDto.Payload(bytes)
            MessageAttachmentType.VOICE -> error("Voice attachments are loaded by the voice-message path")
        }
    }
}
