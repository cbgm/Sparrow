package com.cbgm.sparrow.feature.chats.presentation.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.chats.domain.usecase.group.CreateGroupConversationUseCase
import com.cbgm.sparrow.feature.chats.presentation.create.mapper.toUiState
import com.cbgm.sparrow.feature.chats.presentation.create.model.CreateGroupEffect
import com.cbgm.sparrow.feature.chats.presentation.create.model.CreateGroupUiEvent
import com.cbgm.sparrow.feature.chats.presentation.create.model.CreateGroupUiState
import com.cbgm.sparrow.feature.contacts.domain.model.ContactsWithProfilePictures
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactsWithProfilePicturesUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateGroupViewModel(
    savedStateHandle: SavedStateHandle,
    private val observeContactsWithProfilePictures: ObserveContactsWithProfilePicturesUseCase,
    private val createGroupConversation: CreateGroupConversationUseCase
) : BaseViewModel() {
    private val title = savedStateHandle.getMutableStateFlow(TITLE_KEY, "")
    private val searchQuery = savedStateHandle.getMutableStateFlow(SEARCH_QUERY_KEY, "")
    private val selectedContactIds =
        savedStateHandle.getMutableStateFlow(SELECTED_CONTACT_IDS_KEY, emptyArray<String>())
    private val actionState = MutableStateFlow(CreateGroupActionState())
    private val formState =
        combine(title, searchQuery, selectedContactIds) { title, searchQuery, selectedContactIds ->
            CreateGroupFormState(
                title = title,
                searchQuery = searchQuery,
                selectedContactIds = selectedContactIds.toSet()
            )
        }

    val uiState: StateFlow<CreateGroupUiState> =
        combine(
            contactsPresentationFlow(),
            formState,
            actionState
        ) { snapshot, form, action ->
            snapshot.contacts.contacts.toUiState(
                profilePictures = snapshot.contacts.profilePictures,
                title = form.title,
                searchQuery = form.searchQuery,
                selectedContactIds = form.selectedContactIds,
                isCreating = action.isCreating,
                errorMessage = action.errorMessage ?: snapshot.errorMessage
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CreateGroupUiState()
        )

    private val _effects = Channel<CreateGroupEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onUiEvent(event: CreateGroupUiEvent) {
        when (event) {
            CreateGroupUiEvent.BackClicked -> requestBack()
            is CreateGroupUiEvent.TitleChanged -> updateTitle(event.title)
            is CreateGroupUiEvent.SearchQueryChanged -> updateSearchQuery(event.query)
            is CreateGroupUiEvent.ContactSelected -> toggleContactSelection(event.contactId)
            CreateGroupUiEvent.CreateClicked -> createGroup()
        }
    }

    private fun contactsPresentationFlow() =
        observeContactsWithProfilePictures()
            .map { contacts -> ContactsPresentation(contacts = contacts) }
            .catch { error ->
                emit(
                    ContactsPresentation(
                        contacts = ContactsWithProfilePictures(emptyList(), emptyMap()),
                        errorMessage = error.message ?: "Contacts could not be loaded"
                    )
                )
            }

    private fun requestBack() {
        clearForm()
        _effects.trySend(CreateGroupEffect.BackRequested)
    }

    private fun updateTitle(value: String) {
        title.value = value
        clearActionError()
    }

    private fun updateSearchQuery(query: String) {
        searchQuery.value = query
        clearActionError()
    }

    private fun toggleContactSelection(contactId: String) {
        if (actionState.value.isCreating) return

        selectedContactIds.value =
            selectedContactIds.value.toMutableList().apply {
                if (!remove(contactId)) add(contactId)
            }.toTypedArray()
        clearActionError()
    }

    private fun createGroup() {
        val state = uiState.value
        if (!state.canCreate) return

        viewModelScope.launch {
            actionState.value = CreateGroupActionState(isCreating = true)

            createGroupConversation(state.title, state.selectedContactIds)
                .onSuccess {
                    clearForm()
                    _effects.send(CreateGroupEffect.GroupCreated)
                }.onFailure { error ->
                    actionState.value =
                        CreateGroupActionState(
                            errorMessage = error.message ?: "Group could not be created"
                        )
                }
        }
    }

    private fun clearForm() {
        title.value = ""
        searchQuery.value = ""
        selectedContactIds.value = emptyArray()
        actionState.value = CreateGroupActionState()
    }

    private fun clearActionError() {
        actionState.update { state -> state.copy(errorMessage = null) }
    }

    private data class ContactsPresentation(
        val contacts: ContactsWithProfilePictures,
        val errorMessage: String? = null
    )

    private data class CreateGroupFormState(
        val title: String = "",
        val searchQuery: String = "",
        val selectedContactIds: Set<String> = emptySet()
    )

    private data class CreateGroupActionState(
        val isCreating: Boolean = false,
        val errorMessage: String? = null
    )

    private companion object {
        const val TITLE_KEY = "title"
        const val SEARCH_QUERY_KEY = "searchQuery"
        const val SELECTED_CONTACT_IDS_KEY = "selectedContactIds"
    }
}
