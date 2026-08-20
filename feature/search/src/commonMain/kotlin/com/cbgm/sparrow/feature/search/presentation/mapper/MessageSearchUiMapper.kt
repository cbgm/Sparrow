package com.cbgm.sparrow.feature.search.presentation.mapper

import com.cbgm.sparrow.feature.search.domain.model.MessageSearchResult
import com.cbgm.sparrow.feature.search.domain.model.SemanticSearchState
import com.cbgm.sparrow.feature.search.presentation.formatter.formatMessageSearchTimestamp
import com.cbgm.sparrow.feature.search.presentation.model.MessageSearchMode
import com.cbgm.sparrow.feature.search.presentation.model.MessageSearchResultUiModel

internal fun MessageSearchResult.toUiModel(): MessageSearchResultUiModel =
    MessageSearchResultUiModel(
        messageId = messageId,
        conversationId = conversationId,
        conversationType = conversationType,
        contactId = contactId,
        conversationName = conversationName,
        text = text,
        timestamp = formatMessageSearchTimestamp(createdAtEpochMilliseconds)
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
