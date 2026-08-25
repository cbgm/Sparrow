package com.cbgm.sparrow.feature.search.data.mapper

import com.cbgm.sparrow.data.database.model.StoredMessageEmbedding
import com.cbgm.sparrow.data.database.model.StoredMessageSearchMatch
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchConversationType
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchMatchType
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchResult

internal fun StoredMessageSearchMatch.toExactSearchResult(): MessageSearchResult =
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

internal fun StoredMessageEmbedding.toSemanticSearchResult(score: Float): MessageSearchResult =
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
