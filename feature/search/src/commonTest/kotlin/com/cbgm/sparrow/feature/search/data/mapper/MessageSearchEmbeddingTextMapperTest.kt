package com.cbgm.sparrow.feature.search.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MessageSearchEmbeddingTextMapperTest {
    @Test
    fun conversationTitleIsPreferred() {
        assertEquals(
            "Group chat",
            messageSearchConversationName(
                conversationTitle = " Group chat ",
                contactName = "Contact"
            )
        )
    }

    @Test
    fun contactNameIsUsedForDirectConversation() {
        assertEquals(
            "Contact",
            messageSearchConversationName(
                conversationTitle = null,
                contactName = " Contact "
            )
        )
    }

    @Test
    fun blankNamesProduceNoDisplayName() {
        assertNull(
            messageSearchConversationName(
                conversationTitle = " ",
                contactName = ""
            )
        )
    }
}
