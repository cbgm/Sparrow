package com.cbgm.sparrow.feature.search.presentation.overview

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchConversationType
import com.cbgm.sparrow.feature.search.domain.usecase.ObserveSemanticSearchStateUseCase
import com.cbgm.sparrow.feature.search.domain.usecase.SearchMessagesUseCase
import com.cbgm.sparrow.feature.search.presentation.overview.mapper.toMessageSearchMode
import com.cbgm.sparrow.feature.search.presentation.overview.mapper.toUiModel
import com.cbgm.sparrow.feature.search.presentation.overview.model.MessageSearchUiEvent
import com.cbgm.sparrow.feature.search.presentation.overview.model.MessageSearchUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MessageSearchViewModel(
    observeSemanticSearchState: ObserveSemanticSearchStateUseCase,
    private val searchMessages: SearchMessagesUseCase
) : BaseViewModel() {
    private val mutableUiState = MutableStateFlow(MessageSearchUiState())
    val uiState: StateFlow<MessageSearchUiState> = mutableUiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            observeSemanticSearchState().collect { semanticState ->
                mutableUiState.value =
                    mutableUiState.value.copy(
                        mode = semanticState.toMessageSearchMode()
                    )
            }
        }
    }

    fun onUiEvent(event: MessageSearchUiEvent) {
        when (event) {
            is MessageSearchUiEvent.QueryChanged -> updateQuery(event.query)
            MessageSearchUiEvent.ClearQueryClicked -> updateQuery("")
            is MessageSearchUiEvent.ResultClicked -> openResult(event.messageId)
            MessageSearchUiEvent.BackClicked -> navigator.popBackStack()
        }
    }

    private fun openResult(messageId: String) {
        val result = mutableUiState.value.results.firstOrNull { it.messageId == messageId } ?: return

        when (result.conversationType) {
            MessageSearchConversationType.DIRECT -> {
                val contactId = result.contactId ?: return
                val contactName = result.conversationName ?: return
                navigator.navigateTo(
                    AppRoute.Chat(
                        conversationId = result.conversationId,
                        contactId = contactId,
                        contactName = contactName,
                        targetMessageId = result.messageId
                    )
                )
            }

            MessageSearchConversationType.GROUP ->
                navigator.navigateTo(
                    AppRoute.GroupConversation(
                        conversationId = result.conversationId,
                        targetMessageId = result.messageId
                    )
                )
        }
    }

    private fun updateQuery(query: String) {
        searchJob?.cancel()
        mutableUiState.value =
            mutableUiState.value.copy(
                query = query,
                results = emptyList(),
                isSearching = false,
                searchFailed = false
            )

        if (query.isBlank()) return

        searchJob =
            viewModelScope.launch {
                delay(SEARCH_DEBOUNCE_MILLIS.milliseconds)
                mutableUiState.value = mutableUiState.value.copy(isSearching = true)

                try {
                    val results = searchMessages(query)
                    if (mutableUiState.value.query != query) return@launch
                    mutableUiState.value =
                        mutableUiState.value.copy(
                            results = results.map { it.toUiModel() },
                            isSearching = false,
                            searchFailed = false
                        )
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Throwable) {
                    if (mutableUiState.value.query != query) return@launch
                    mutableUiState.value =
                        mutableUiState.value.copy(
                            results = emptyList(),
                            isSearching = false,
                            searchFailed = true
                        )
                }
            }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 250L
    }
}
