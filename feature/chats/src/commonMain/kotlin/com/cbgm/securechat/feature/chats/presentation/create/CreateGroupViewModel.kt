package com.cbgm.securechat.feature.chats.presentation.create

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.chats.domain.usecase.CreateGroupConversationUseCase
import com.cbgm.securechat.feature.chats.presentation.create.model.CreateGroupEffect
import com.cbgm.securechat.feature.chats.presentation.create.model.CreateGroupUiEvent
import com.cbgm.securechat.feature.chats.presentation.create.model.CreateGroupUiState
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContacts
import com.cbgm.securechat.feature.contacts.presentation.mapper.filterContacts
import com.cbgm.securechat.feature.contacts.presentation.mapper.groupContactsByInitial
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateGroupViewModel(
    private val observeContacts: ObserveContacts,
    private val createGroupConversation: CreateGroupConversationUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    private val _effects = Channel<CreateGroupEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var contacts: List<Contact> = emptyList()

    init {
        observeAvailableContacts()
    }

    fun onUiEvent(event: CreateGroupUiEvent) {
        when (event) {
            CreateGroupUiEvent.BackClicked -> requestBack()
            is CreateGroupUiEvent.TitleChanged -> updateTitle(event.title)
            is CreateGroupUiEvent.SearchQueryChanged -> updateSearchQuery(event.query)
            is CreateGroupUiEvent.ContactSelected -> toggleContactSelection(event.contactId)
            CreateGroupUiEvent.CreateClicked -> createGroup()
        }
    }

    private fun requestBack() {
        clearData()
        _effects.trySend(CreateGroupEffect.BackRequested)
    }

    private fun clearData() {
        _uiState.update { CreateGroupUiState() }
    }

    private fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, errorMessage = null) }
    }

    private fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                contactGroups = contacts.filterContacts(query).groupContactsByInitial()
            )
        }
    }

    private fun toggleContactSelection(contactId: String) {
        _uiState.update { state ->
            val selectedContactIds =
                state.selectedContactIds.toMutableSet().apply {
                    if (!add(contactId)) remove(contactId)
                }

            state.copy(
                selectedContactIds = selectedContactIds,
                errorMessage = null
            )
        }
    }

    private fun createGroup() {
        val state = _uiState.value
        if (!state.canCreate || state.isCreating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, errorMessage = null) }

            createGroupConversation(state.title, state.selectedContactIds)
                .onSuccess { conversationId ->
                    _uiState.update { it.copy(isCreating = false) }
                    _effects.send(CreateGroupEffect.GroupCreated(conversationId))
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            errorMessage = error.message ?: "Group could not be created"
                        )
                    }
                }
        }
    }

    private fun observeAvailableContacts() {
        viewModelScope.launch {
            observeContacts()
                .catch { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Contacts could not be loaded")
                    }
                }.collect { observedContacts ->
                    contacts = observedContacts
                    _uiState.update { state ->
                        state.copy(
                            contactGroups =
                                contacts
                                    .filterContacts(state.searchQuery)
                                    .groupContactsByInitial(),
                            selectedContactIds =
                                state.selectedContactIds.filterTo(mutableSetOf()) { contactId ->
                                    contacts.any { it.id == contactId }
                                }
                        )
                    }
                }
        }
    }
}
