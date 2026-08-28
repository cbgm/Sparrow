package com.cbgm.sparrow.feature.search.data.mapper

import com.cbgm.sparrow.data.database.model.StoredMessageEmbeddingDto
import com.cbgm.sparrow.data.database.model.StoredMessageSearchMatchDto
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchConversationType
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchMatchType
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchResult

internal fun StoredMessageSearchMatchDto.toMessageSearchResult(): MessageSearchResult =
    MessageSearchResult(
        messageId = messageId,
        conversationId = conversationId,
        conversationType = MessageSearchConversationType.valueOf(conversationType),
        contactId = contactId,
        conversationName = messageSearchConversationName(conversationTitle, contactName),
        text = text,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
        matchType = MessageSearchMatchType.EXACT
    )

internal fun StoredMessageEmbeddingDto.toMessageSearchResult(score: Float): MessageSearchResult =
    MessageSearchResult(
        messageId = messageId,
        conversationId = conversationId,
        conversationType = MessageSearchConversationType.valueOf(conversationType),
        contactId = contactId,
        conversationName = messageSearchConversationName(conversationTitle, contactName),
        text = text,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
        matchType = MessageSearchMatchType.SEMANTIC,
        score = score
    )
