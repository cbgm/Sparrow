package com.cbgm.sparrow.feature.attachments.data.datasource

import com.cbgm.sparrow.protocol.attachment.GroupPinnedAttachmentProvider

internal class AttachmentContentDataSource(
    private val messageAttachmentDataSource: MessageAttachmentDataSource,
    private val messageAttachmentFileDataSource: MessageAttachmentFileDataSource,
    private val groupPinnedAttachmentProvider: GroupPinnedAttachmentProvider
) {
    suspend fun loadLocalFile(partId: String, groupId: String?): String {
        require(partId.isNotBlank()) { "Message part ID must not be blank" }
        if (groupId == null) {
            messageAttachmentDataSource.resolveLocalFilePath(partId)?.let { localFilePath ->
                return localFilePath
            }

            messageAttachmentDataSource.loadBytes(partId)
            return requireNotNull(messageAttachmentDataSource.resolveLocalFilePath(partId)) {
                "Message part was loaded but no local cache file exists"
            }
        }

        val bytes = groupPinnedAttachmentProvider.load(groupId, partId)
        val fileName = messageAttachmentFileDataSource.write(bytes)
        return requireNotNull(messageAttachmentFileDataSource.resolveCacheFilePath(fileName)) {
            "Pinned message-part cache file could not be resolved"
        }
    }

    suspend fun loadBytes(partId: String, groupId: String?): ByteArray {
        require(partId.isNotBlank()) { "Message part ID must not be blank" }
        return if (groupId == null) {
            messageAttachmentDataSource.loadBytes(partId)
        } else {
            groupPinnedAttachmentProvider.load(groupId, partId)
        }
    }
}
