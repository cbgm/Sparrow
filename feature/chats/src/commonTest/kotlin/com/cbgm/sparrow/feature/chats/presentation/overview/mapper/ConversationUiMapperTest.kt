package com.cbgm.sparrow.feature.chats.presentation.overview.mapper

import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverview
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewPreview
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewType
import com.cbgm.sparrow.feature.chats.presentation.overview.model.LastMessagePreviewUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConversationUiMapperTest {
    @Test
    fun attachmentOnlyMessagesHavePreviewForDirectAndGroup() {
        for (type in ConversationOverviewType.entries) {
            val expected = mapOf(
                ConversationOverviewPreview.MEDIA to LastMessagePreviewUi.MEDIA,
                ConversationOverviewPreview.LOCATION to LastMessagePreviewUi.LOCATION,
                ConversationOverviewPreview.CONTACT_CARD to LastMessagePreviewUi.CONTACT_CARD,
                ConversationOverviewPreview.VOICE to LastMessagePreviewUi.VOICE,
                ConversationOverviewPreview.ATTACHMENT to LastMessagePreviewUi.ATTACHMENT
            )
            for ((attachment, uiPreview) in expected) {
                val item = conversation(type = type, preview = attachment).toConversationListItem()
                assertEquals(uiPreview, item.lastMessagePreview)
                assertTrue(item.hasMessages)
            }
        }
    }

    @Test
    fun emptyConversationHasNoPreviewOrMessage() {
        val item = conversation(preview = null, timestamp = null).toConversationListItem()
        assertEquals(null, item.lastMessagePreview)
        assertEquals(false, item.hasMessages)
    }

    private fun conversation(
        type: ConversationOverviewType = ConversationOverviewType.DIRECT,
        preview: ConversationOverviewPreview?,
        timestamp: Long? = 123L
    ): ConversationOverview = ConversationOverview(
        id = "conversation-id",
        contactId = "contact-id",
        displayName = "Contact",
        lastMessageText = "",
        lastMessageTimestamp = timestamp,
        updatedAtEpochMilliseconds = 123L,
        unreadCount = 0,
        participantCount = 2,
        type = type,
        lastMessagePreview = preview
    )
}
