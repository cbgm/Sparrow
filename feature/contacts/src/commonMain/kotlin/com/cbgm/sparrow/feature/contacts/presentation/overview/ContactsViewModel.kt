package com.cbgm.sparrow.feature.contacts.presentation.overview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.contacts.domain.usecase.ImportDeviceContactsUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactsUseCase
import com.cbgm.sparrow.feature.contacts.presentation.overview.mapper.toContactsUiState
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsEffect
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsUiEvent
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsViewModel(
    savedStateHandle: SavedStateHandle,
    observeContacts: ObserveContactsUseCase,
    private val importDeviceContacts: ImportDeviceContactsUseCase
) : BaseViewModel() {
    private val searchQuery = savedStateHandle.getMutableStateFlow(SEARCH_QUERY_KEY, "")

    private val _effects = Channel<ContactsEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState: StateFlow<ContactsUiState> =
        combine(
            observeContacts(),
            searchQuery
        ) { contacts, query ->
            contacts.toContactsUiState(query)
        }.catch { error ->
            reportError(error.message ?: "Failed to load contacts")
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

    fun reportError(message: String) = SparrowLog.error("ContactsViewModel", message)

    private fun importContacts() {
        viewModelScope.launch {
            importDeviceContacts()
                .onFailure { error ->
                    SparrowLog.error("ContactsViewModel", "Failed to import contacts", error)
                }
        }
    }

    private fun showPermissionDenied() {
        SparrowLog.error("ContactsViewModel", "Contacts permission is required to import device contacts.")
    }

    private fun emitEffect(effect: ContactsEffect) {
        _effects.trySend(effect)
    }

    private companion object {
        const val SEARCH_QUERY_KEY = "searchQuery"
    }
}
