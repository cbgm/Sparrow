package com.cbgm.sparrow.protocol.attachment

interface GroupPinnedAttachmentSource {
    suspend fun load(
        groupId: String,
        attachmentId: String
    ): ByteArray
}
