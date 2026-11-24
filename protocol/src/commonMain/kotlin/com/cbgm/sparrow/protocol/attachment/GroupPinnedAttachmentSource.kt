package com.cbgm.sparrow.protocol.attachment

interface GroupPinnedAttachmentSource {
    /** Whether [attachmentId] belongs to the message pinned in group [groupId], so its bytes may be loaded for it. */
    suspend fun isPinned(
        groupId: String,
        attachmentId: String
    ): Boolean
}
