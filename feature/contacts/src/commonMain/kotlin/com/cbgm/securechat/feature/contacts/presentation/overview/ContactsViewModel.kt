package com.cbgm.securechat.feature.contacts.presentation.overview

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportDeviceContactsUseCase
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContactsUseCase
import com.cbgm.securechat.feature.contacts.presentation.overview.mapper.filterContacts
import com.cbgm.securechat.feature.contacts.presentation.overview.mapper.groupContactsByInitial
import com.cbgm.securechat.feature.contacts.presentation.overview.model.ContactsEffect
import com.cbgm.securechat.feature.contacts.presentation.overview.model.ContactsUiEvent
import com.cbgm.securechat.feature.contacts.presentation.overview.model.ContactsUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val observeContacts: ObserveContactsUseCase,
    private val importDeviceContacts: ImportDeviceContactsUseCase
) : BaseViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _effects = Channel<ContactsEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState: StateFlow<ContactsUiState> =
        combine(
            observeContacts(),
            searchQuery
        ) { contacts, query ->
            contacts.toUiState(query)
        }.catch { error ->
            emit(ContactsUiState.Error(error.message ?: "Failed to load contacts"))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ContactsUiState.Loading
        )

    fun onUiEvent(event: ContactsUiEvent) {
        when (event) {
            is ContactsUiEvent.SearchQueryChanged -> _searchQuery.value = event.query
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
        viewModelScope.launch {
            _effects.send(
                ContactsEffect.ShowError(
                    "Contacts permission is required to import device contacts."
                )
            )
        }
    }

    private fun emitEffect(effect: ContactsEffect) {
        _effects.trySend(effect)
    }

    private fun List<Contact>.toUiState(query: String): ContactsUiState {
        if (isEmpty()) return ContactsUiState.Empty

        return ContactsUiState.Content(
            groups = filterContacts(query).groupContactsByInitial()
        )
    }
}
