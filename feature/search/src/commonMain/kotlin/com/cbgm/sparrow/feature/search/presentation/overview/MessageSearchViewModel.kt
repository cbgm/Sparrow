package com.cbgm.sparrow.feature.search.presentation.overview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchConversationType
import com.cbgm.sparrow.feature.search.domain.usecase.ObserveSemanticSearchStateUseCase
import com.cbgm.sparrow.feature.search.domain.usecase.SearchMessagesUseCase
import com.cbgm.sparrow.feature.search.presentation.overview.mapper.toMessageSearchMode
import com.cbgm.sparrow.feature.search.presentation.overview.mapper.toMessageSearchResultUi
import com.cbgm.sparrow.feature.search.presentation.overview.model.MessageSearchResultUi
import com.cbgm.sparrow.feature.search.presentation.overview.model.MessageSearchUiEvent
import com.cbgm.sparrow.feature.search.presentation.overview.model.MessageSearchUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MessageSearchViewModel(
    savedStateHandle: SavedStateHandle? = null,
    observeSemanticSearchState: ObserveSemanticSearchStateUseCase,
    private val searchMessages: SearchMessagesUseCase
) : BaseViewModel() {
    private val conversationId: String? = savedStateHandle?.get<String>("conversationId")
    private val queryFlow = MutableStateFlow("")
    private val cachedSearchState = MutableStateFlow(SearchExecutionState())

    init {
        // Runs once per unique debounced query and caches results in memory.
        // Does NOT re-trigger when returning from the background.
        queryFlow
            .debounce(SEARCH_DEBOUNCE_MILLIS.milliseconds)
            .map { it.trim() }
            .distinctUntilChanged()
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    flowOf(SearchExecutionState())
                } else {
                    flow {
                        emit(cachedSearchState.value.copy(isSearching = true, searchFailed = false))
                        searchMessages(query = query, conversationId = conversationId).fold(
                            onSuccess = { domainResults ->
                                emit(
                                    SearchExecutionState(
                                        results = domainResults.map { it.toMessageSearchResultUi() },
                                        isSearching = false,
                                        searchFailed = false
                                    )
                                )
                            },
                            onFailure = { failure ->
                                if (failure is CancellationException) throw failure
                                SparrowLog.error("MessageSearchViewModel", "Message search failed", failure)
                                emit(
                                    SearchExecutionState(
                                        results = emptyList(),
                                        isSearching = false,
                                        searchFailed = true
                                    )
                                )
                            }
                        )
                    }
                }
            }
            .onEach { newState -> cachedSearchState.value = newState }
            .launchIn(viewModelScope)
    }

    // Only observeSemanticSearchState() is paused 5s after backgrounding;
    // queryFlow and cachedSearchState are StateFlows in memory and re-emit immediately on resume.
    val uiState: StateFlow<MessageSearchUiState> = combine(
        queryFlow,
        cachedSearchState,
        observeSemanticSearchState()
    ) { rawQuery, executionState, semanticState ->
        MessageSearchUiState(
            query = rawQuery,
            results = if (rawQuery.isBlank()) emptyList() else executionState.results,
            isSearching = executionState.isSearching,
            searchFailed = executionState.searchFailed,
            mode = semanticState.toMessageSearchMode()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = MessageSearchUiState()
    )

    fun onUiEvent(event: MessageSearchUiEvent) {
        when (event) {
            is MessageSearchUiEvent.QueryChanged -> updateQuery(event.query)
            MessageSearchUiEvent.ClearQueryClicked -> updateQuery("")
            is MessageSearchUiEvent.ResultClicked -> openResult(event.messageId)
            MessageSearchUiEvent.BackClicked -> navigator.popBackStack()
        }
    }

    private fun updateQuery(query: String) {
        queryFlow.update { query }
    }

    private fun openResult(messageId: String) {
        val result = uiState.value.results.firstOrNull { it.messageId == messageId } ?: return
        val searchRoute = AppRoute.MessageSearch(conversationId = conversationId)

        when (result.conversationType) {
            MessageSearchConversationType.DIRECT -> {
                val contactId = result.contactId ?: return
                val contactName = result.conversationName ?: return
                navigator.navigateTo(
                    route = AppRoute.Chat(
                        conversationId = result.conversationId,
                        contactId = contactId,
                        contactName = contactName,
                        targetMessageId = result.messageId
                    ),
                    popUpTo = searchRoute,
                    inclusive = true
                )
            }

            MessageSearchConversationType.GROUP ->
                navigator.navigateTo(
                    route = AppRoute.GroupConversation(
                        conversationId = result.conversationId,
                        targetMessageId = result.messageId
                    ),
                    popUpTo = searchRoute,
                    inclusive = true
                )
        }
    }

    private data class SearchExecutionState(
        val results: List<MessageSearchResultUi> = emptyList(),
        val isSearching: Boolean = false,
        val searchFailed: Boolean = false
    )

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 250L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
