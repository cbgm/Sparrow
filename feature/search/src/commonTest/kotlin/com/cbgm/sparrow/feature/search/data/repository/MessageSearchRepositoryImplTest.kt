package com.cbgm.sparrow.feature.search.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MessageSearchRepositoryImplTest {
    @Test
    fun conversationTitleIsPreferred() {
        assertEquals(
            "Group chat",
            conversationDisplayName(
                conversationTitle = " Group chat ",
                contactName = "Contact"
            )
        )
    }

    @Test
    fun contactNameIsUsedForDirectConversation() {
        assertEquals(
            "Contact",
            conversationDisplayName(
                conversationTitle = null,
                contactName = " Contact "
            )
        )
    }

    @Test
    fun blankNamesProduceNoDisplayName() {
        assertNull(
            conversationDisplayName(
                conversationTitle = " ",
                contactName = ""
            )
        )
    }
}
