package com.cbgm.sparrow.core.protocol.attachment

interface GroupPinnedAttachmentProvider {
    suspend fun load(
        groupId: String,
        attachmentId: String
    ): ByteArray
}
