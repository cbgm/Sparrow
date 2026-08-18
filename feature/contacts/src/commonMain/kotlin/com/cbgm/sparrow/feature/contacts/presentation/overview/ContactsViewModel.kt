package com.cbgm.sparrow.feature.contacts.presentation.overview

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.usecase.ImportDeviceContactsUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactProfilePicturesUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactsUseCase
import com.cbgm.sparrow.feature.contacts.presentation.overview.mapper.toUiState
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsEffect
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsUiEvent
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsUiState
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsViewModel(
    private val observeContacts: ObserveContactsUseCase,
    private val importDeviceContacts: ImportDeviceContactsUseCase,
    private val observeProfilePictures: ObserveContactProfilePicturesUseCase
) : BaseViewModel() {
    private val searchQuery = MutableStateFlow("")

    private val _effects = Channel<ContactsEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState: StateFlow<ContactsUiState> =
        combine(
            observeContactsWithProfilePictures(),
            searchQuery
        ) { snapshot, query ->
            snapshot.contacts.toUiState(query, snapshot.profilePictures)
        }.catch { error ->
            emit(ContactsUiState.Error(error.message ?: "Failed to load contacts", searchQuery.value))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ContactsUiState.Loading()
        )

    fun onUiEvent(event: ContactsUiEvent) {
        when (event) {
            is ContactsUiEvent.SearchQueryChanged -> searchQuery.value = event.query
            ContactsUiEvent.ImportDeviceContacts -> importContacts()
            ContactsUiEvent.DeviceContactsPermissionDenied -> showPermissionDenied()
            ContactsUiEvent.BackClicked -> emitEffect(ContactsEffect.BackRequested)
            ContactsUiEvent.ImportContactClicked -> emitEffect(ContactsEffect.ImportContactRequested)
            ContactsUiEvent.CreateGroupClicked -> emitEffect(ContactsEffect.CreateGroupRequested)
            is ContactsUiEvent.ContactClicked -> {
                emitEffect(
                    ContactsEffect.ContactSelected(
                        contactId = event.contactId,
                        contactName = event.contactName
                    )
                )
            }
            is ContactsUiEvent.SelectionTitleChanged,
            is ContactsUiEvent.ContactSelectionToggled,
            ContactsUiEvent.SelectionConfirmed -> Unit
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
            }

    private fun importContacts() {
        viewModelScope.launch {
            importDeviceContacts()
                .onFailure { error ->
                    _effects.send(
                        ContactsEffect.ShowError(error.message ?: "Failed to import contacts")
                    )
                }
        }
    }

    private fun showPermissionDenied() {
        emitEffect(
            ContactsEffect.ShowError(
                "Contacts permission is required to import device contacts."
            )
        )
    }

    private fun emitEffect(effect: ContactsEffect) {
        _effects.trySend(effect)
    }

    private data class ContactsSnapshot(
        val contacts: List<Contact>,
        val profilePictures: Map<String, ByteArray?>
    )
}
