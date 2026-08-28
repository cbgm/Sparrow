package com.cbgm.sparrow.feature.search.presentation.overview.mapper

import com.cbgm.sparrow.core.time.formatMessageTimestamp
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchResult
import com.cbgm.sparrow.feature.search.domain.model.SemanticSearchState
import com.cbgm.sparrow.feature.search.presentation.overview.model.MessageSearchMode
import com.cbgm.sparrow.feature.search.presentation.overview.model.MessageSearchResultUi

internal fun MessageSearchResult.toMessageSearchResultUi(): MessageSearchResultUi =
    MessageSearchResultUi(
        messageId = messageId,
        conversationId = conversationId,
        conversationType = conversationType,
        contactId = contactId,
        conversationName = conversationName,
        text = text,
        timestamp = formatMessageTimestamp(createdAtEpochMilliseconds)
    )

internal fun SemanticSearchState.toMessageSearchMode(): MessageSearchMode =
    when (this) {
        SemanticSearchState.Ready -> MessageSearchMode.HYBRID
        SemanticSearchState.Disabled -> MessageSearchMode.EXACT_ONLY
        SemanticSearchState.Preparing,
        is SemanticSearchState.DownloadingModel,
        is SemanticSearchState.BuildingIndex -> MessageSearchMode.PREPARING_SEMANTIC
        is SemanticSearchState.Failed -> MessageSearchMode.SEMANTIC_UNAVAILABLE
    }
