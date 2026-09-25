package com.cbgm.sparrow.feature.chats.data.overview.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.data.database.entity.ConversationType
import com.cbgm.sparrow.data.database.model.ConversationSummaryDto
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewPreview
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationOverviewMapperTest {
    @Test
    fun identifiesLatestMessageAttachments() {
        val cases = mapOf(
            MessageAttachmentType.IMAGE to ConversationOverviewPreview.MEDIA,
            MessageAttachmentType.VIDEO to ConversationOverviewPreview.MEDIA,
            MessageAttachmentType.FILE to ConversationOverviewPreview.MEDIA,
            MessageAttachmentType.LOCATION to ConversationOverviewPreview.LOCATION,
            MessageAttachmentType.CONTACT to ConversationOverviewPreview.CONTACT_CARD,
            MessageAttachmentType.VOICE to ConversationOverviewPreview.VOICE
        )
        cases.forEach { (type, expected) ->
            assertEquals(expected, summary(type).toConversationOverview().lastMessagePreview)
        }
        assertEquals(null, summary(null).toConversationOverview().lastMessagePreview)
    }

    private fun summary(attachmentType: MessageAttachmentType?) = ConversationSummaryDto(
        conversationId = "conversation",
        contactId = "contact",
        contactName = "Contact",
        conversationType = ConversationType.DIRECT.name,
        conversationTitle = null,
        participantCount = 2,
        lastMessageText = "",
        unreadCount = 0,
        lastMessageTimestamp = 12L,
        updatedAtEpochMilliseconds = 12L,
        lastMessageAttachmentType = attachmentType
    )
}
