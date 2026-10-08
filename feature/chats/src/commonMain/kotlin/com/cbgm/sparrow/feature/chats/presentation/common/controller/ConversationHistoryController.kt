package com.cbgm.sparrow.feature.chats.presentation.common.controller

import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryCursor
import com.cbgm.sparrow.feature.chats.domain.usecase.FindMessageHistoryCursorUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.forward.LoadOlderMessagesUseCase
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageHistoryUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConversationHistoryController(
    private val loadOlderMessages: LoadOlderMessagesUseCase,
    private val findMessageCursor: FindMessageHistoryCursorUseCase
) {
    private val _cursor = MutableStateFlow<MessageHistoryCursor?>(null)
    private val observedCursor = MutableStateFlow<MessageHistoryCursor?>(null)
    val cursor: StateFlow<MessageHistoryCursor?> = _cursor.asStateFlow()

    fun markObserved(cursor: MessageHistoryCursor?) {
        observedCursor.value = cursor
    }

    private val isLoading = MutableStateFlow(false)
    private val hasMore = MutableStateFlow(true)

    fun observe(scope: CoroutineScope): StateFlow<MessageHistoryUiState> =
        combine(isLoading, hasMore, observedCursor) { loading, more, observed ->
            MessageHistoryUiState(loading, more, observed?.messageId)
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), MessageHistoryUiState())

    fun loadOlder(scope: CoroutineScope, conversationId: String, onError: (Throwable) -> Unit) {
        if (isLoading.value || !hasMore.value) return
        isLoading.value = true
        scope.launch {
            try {
                loadOlderMessages(
                    conversationId = conversationId,
                    currentOldestCursor = cursor.value
                )
                    .onSuccess { page ->
                        page.oldestCursor?.let { _cursor.value = it }
                        hasMore.value = page.hasMore
                    }
                    .onFailure(onError)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun requestTarget(
        scope: CoroutineScope,
        conversationId: String,
        messageId: String,
        onError: (Throwable) -> Unit
    ) {
        scope.launch {
            findMessageCursor(conversationId, messageId)
                .onSuccess { target ->
                    if (target != null && (cursor.value == null || target.isOlderThan(cursor.value!!))) {
                        _cursor.value = target
                    }
                }.onFailure(onError)
        }
    }

    fun ensureTargetMessageLoaded(
        scope: CoroutineScope,
        conversationId: String,
        messageId: String?,
        ready: Flow<Boolean>,
        containsMessage: () -> Boolean,
        onError: (Throwable) -> Unit
    ) {
        if (messageId == null) return
        scope.launch {
            ready.filter { it }.first()
            if (containsMessage()) return@launch
            findMessageCursor(conversationId, messageId)
                .onSuccess { target -> target?.let { _cursor.value = it } }
                .onFailure(onError)
        }
    }
}
