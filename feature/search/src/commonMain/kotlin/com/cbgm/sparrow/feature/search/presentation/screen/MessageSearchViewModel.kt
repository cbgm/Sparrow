package com.cbgm.sparrow.feature.search.presentation.screen

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.search.domain.usecase.ObserveSemanticSearchStateUseCase
import com.cbgm.sparrow.feature.search.domain.usecase.SearchMessagesUseCase
import com.cbgm.sparrow.feature.search.presentation.mapper.toMessageSearchMode
import com.cbgm.sparrow.feature.search.presentation.mapper.toUiModel
import com.cbgm.sparrow.feature.search.presentation.model.MessageSearchUiEvent
import com.cbgm.sparrow.feature.search.presentation.model.MessageSearchUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
            MessageSearchUiEvent.BackClicked -> navigator.popBackStack()
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
                delay(SEARCH_DEBOUNCE_MILLIS)
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
