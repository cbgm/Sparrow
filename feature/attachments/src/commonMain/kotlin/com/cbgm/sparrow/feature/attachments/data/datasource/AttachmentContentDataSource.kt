package com.cbgm.sparrow.feature.attachments.data.datasource

import com.cbgm.sparrow.protocol.attachment.GroupPinnedAttachmentSource

internal class AttachmentContentDataSource(
    private val messageAttachmentDataSource: MessageAttachmentDataSource,
    private val groupPinnedAttachmentSource: GroupPinnedAttachmentSource
) {
    suspend fun loadLocalFile(partId: String, groupId: String?): String {
        require(partId.isNotBlank()) { "Message part ID must not be blank" }
        messageAttachmentDataSource.resolveLocalFilePath(partId)?.let { localFilePath ->
            return localFilePath
        }

        loadBytes(partId, groupId)

        return requireNotNull(messageAttachmentDataSource.resolveLocalFilePath(partId)) {
            "Message part was loaded but no local cache file exists"
        }
    }

    suspend fun loadBytes(partId: String, groupId: String?): ByteArray {
        require(partId.isNotBlank()) { "Message part ID must not be blank" }
        if (groupId == null) return messageAttachmentDataSource.loadBytes(partId)
        check(groupPinnedAttachmentSource.isPinned(groupId, partId)) { "Pinned message attachment was not found" }
        return messageAttachmentDataSource.loadDetachedBytes(partId)
    }
}
