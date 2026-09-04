package com.cbgm.sparrow.feature.chats.presentation.forwarding

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.chats.domain.usecase.overview.ObserveConversationOverviewContextUseCase
import com.cbgm.sparrow.feature.chats.presentation.forwarding.mapper.toForwardingSelectionUiState
import com.cbgm.sparrow.feature.chats.presentation.forwarding.model.ForwardingSelectionEffect
import com.cbgm.sparrow.feature.chats.presentation.forwarding.model.ForwardingSelectionUiEvent
import com.cbgm.sparrow.feature.chats.presentation.forwarding.model.ForwardingSelectionUiState
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactsWithProfilePicturesUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ForwardingSelectionViewModel(
    observeConversationContext: ObserveConversationOverviewContextUseCase,
    observeContactsWithProfilePictures: ObserveContactsWithProfilePicturesUseCase
) : BaseViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val _effects = Channel<ForwardingSelectionEffect>(Channel.BUFFERED)

    val effects = _effects.receiveAsFlow()

    val uiState: StateFlow<ForwardingSelectionUiState> =
        combine(
            observeConversationContext(),
            observeContactsWithProfilePictures(),
            searchQuery
        ) { conversationContext, contactsContext, query ->
            toForwardingSelectionUiState(
                conversationContext = conversationContext,
                contactsContext = contactsContext,
                query = query
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ForwardingSelectionUiState.Loading()
        )

    fun onUiEvent(event: ForwardingSelectionUiEvent) {
        when (event) {
            is ForwardingSelectionUiEvent.SearchQueryChanged ->
                searchQuery.value = event.query

            is ForwardingSelectionUiEvent.TargetClicked ->
                viewModelScope.launch {
                    _effects.send(ForwardingSelectionEffect.TargetSelected(event.target.target))
                }
        }
    }
}
