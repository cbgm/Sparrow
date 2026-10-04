package com.cbgm.sparrow.protocol.attachment

interface GroupPinnedAttachmentProvider {
    suspend fun load(
        groupId: String,
        attachmentId: String
    ): ByteArray
}
