package com.cbgm.sparrow.feature.chats.presentation.create

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.chats.domain.usecase.group.CreateGroupConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.profile.ObserveRemoteProfilePicturesUseCase
import com.cbgm.sparrow.feature.chats.presentation.create.mapper.toUiState
import com.cbgm.sparrow.feature.chats.presentation.create.model.CreateGroupEffect
import com.cbgm.sparrow.feature.chats.presentation.create.model.CreateGroupUiEvent
import com.cbgm.sparrow.feature.chats.presentation.create.model.CreateGroupUiState
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CreateGroupViewModel(
    private val observeContacts: ObserveContactsUseCase,
    private val observeProfilePictures: ObserveRemoteProfilePicturesUseCase,
    private val createGroupConversation: CreateGroupConversationUseCase
) : BaseViewModel() {
    private val formState = MutableStateFlow(CreateGroupFormState())

    val uiState: StateFlow<CreateGroupUiState> =
        combine(
            observeContactsWithProfilePictures(),
            formState
        ) { snapshot, form ->
            snapshot.contacts.toUiState(
                profilePictures = snapshot.profilePictures,
                title = form.title,
                searchQuery = form.searchQuery,
                selectedContactIds = form.selectedContactIds,
                isCreating = form.isCreating,
                errorMessage = form.errorMessage ?: snapshot.errorMessage
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

    private fun observeContactsWithProfilePictures(): Flow<ContactsSnapshot> =
        observeContacts()
            .flatMapLatest { contacts ->
                observeProfilePictures(contacts.mapTo(mutableSetOf(), Contact::id))
                    .map { profilePictures ->
                        ContactsSnapshot(
                            contacts = contacts,
                            profilePictures = profilePictures
                        )
                    }
            }.catch { error ->
                emit(
                    ContactsSnapshot(
                        contacts = emptyList(),
                        profilePictures = emptyMap(),
                        errorMessage = error.message ?: "Contacts could not be loaded"
                    )
                )
            }

    private fun requestBack() {
        formState.value = CreateGroupFormState()
        _effects.trySend(CreateGroupEffect.BackRequested)
    }

    private fun updateTitle(title: String) {
        formState.update { it.copy(title = title, errorMessage = null) }
    }

    private fun updateSearchQuery(query: String) {
        formState.update { it.copy(searchQuery = query, errorMessage = null) }
    }

    private fun toggleContactSelection(contactId: String) {
        formState.update { state ->
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
        val state = uiState.value
        if (!state.canCreate) return

        viewModelScope.launch {
            formState.update { it.copy(isCreating = true, errorMessage = null) }

            createGroupConversation(state.title, state.selectedContactIds)
                .onSuccess { conversationId ->
                    formState.update { it.copy(isCreating = false) }
                    _effects.send(CreateGroupEffect.GroupCreated(conversationId))
                }.onFailure { error ->
                    formState.update {
                        it.copy(
                            isCreating = false,
                            errorMessage = error.message ?: "Group could not be created"
                        )
                    }
                }
        }
    }

    private data class ContactsSnapshot(
        val contacts: List<Contact>,
        val profilePictures: Map<String, ByteArray?>,
        val errorMessage: String? = null
    )

    private data class CreateGroupFormState(
        val title: String = "",
        val searchQuery: String = "",
        val selectedContactIds: Set<String> = emptySet(),
        val isCreating: Boolean = false,
        val errorMessage: String? = null
    )
}
