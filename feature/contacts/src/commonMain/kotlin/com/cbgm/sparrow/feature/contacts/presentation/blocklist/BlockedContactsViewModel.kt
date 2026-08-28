package com.cbgm.sparrow.feature.contacts.presentation.blocklist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.contacts.domain.usecase.BlockContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveBlockedContactsContextUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.UnblockContactUseCase
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.mapper.toBlockedContactsUiState
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.model.BlockedContactsEffect
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.model.BlockedContactsUiEvent
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.model.BlockedContactsUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BlockedContactsViewModel(
    savedStateHandle: SavedStateHandle,
    observeBlockedContactsContext: ObserveBlockedContactsContextUseCase,
    private val blockContact: BlockContactUseCase,
    private val unblockContact: UnblockContactUseCase
) : BaseViewModel() {
    private val showAddContacts =
        savedStateHandle.getMutableStateFlow(SHOW_ADD_CONTACTS_KEY, false)
    private val phoneNumber =
        savedStateHandle.getMutableStateFlow(PHONE_NUMBER_KEY, "")
    private val actionState = MutableStateFlow(BlockedContactsActionState())
    private val formState =
        combine(showAddContacts, phoneNumber) { showAddContacts, phoneNumber ->
            BlockedContactsFormState(
                showAddContacts = showAddContacts,
                phoneNumber = phoneNumber
            )
        }

    val uiState: StateFlow<BlockedContactsUiState> =
        combine(
            observeBlockedContactsContext(),
            formState,
            actionState
        ) { context, form, action ->
            context.blocklist.toBlockedContactsUiState(
                profilePictures = context.profilePictures,
                showAddContacts = form.showAddContacts,
                phoneNumber = form.phoneNumber,
                phoneNumberError = action.phoneNumberError,
                processingContactId = action.processingContactId
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = BlockedContactsUiState()
        )

    private val _effects = Channel<BlockedContactsEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onUiEvent(event: BlockedContactsUiEvent) {
        when (event) {
            BlockedContactsUiEvent.BackClicked -> navigator.popBackStack()
            BlockedContactsUiEvent.AddContactClicked -> openAddContacts()
            BlockedContactsUiEvent.AddContactsDismissed -> closeAddContacts()
            is BlockedContactsUiEvent.PhoneNumberChanged -> updatePhoneNumber(event.value)
            BlockedContactsUiEvent.BlockPhoneNumberClicked -> blockPhoneNumber()
            is BlockedContactsUiEvent.BlockContactClicked -> block(event.contactId)
            is BlockedContactsUiEvent.UnblockContactClicked -> unblock(event.contactId)
        }
    }

    private fun openAddContacts() {
        showAddContacts.value = true
        phoneNumber.value = ""
        clearPhoneNumberError()
    }

    private fun closeAddContacts() {
        clearAddContactForm()
    }

    private fun updatePhoneNumber(value: String) {
        phoneNumber.value = value
        clearPhoneNumberError()
    }

    private fun block(contactId: String) {
        updateContact(contactId) {
            blockContact(contactId)
        }
    }

    private fun blockPhoneNumber() {
        val candidatePhoneNumber = phoneNumber.value.trim()
        if (candidatePhoneNumber.isEmpty() || actionState.value.processingContactId != null) return

        viewModelScope.launch {
            actionState.update {
                it.copy(
                    processingContactId = PHONE_NUMBER_OPERATION_ID,
                    phoneNumberError = null
                )
            }

            blockContact
                .byPhoneNumber(candidatePhoneNumber)
                .onSuccess {
                    clearAddContactForm()
                }.onFailure { error ->
                    actionState.update {
                        it.copy(
                            phoneNumberError = error.message ?: "Phone number could not be blocked"
                        )
                    }
                }

            actionState.update { it.copy(processingContactId = null) }
        }
    }

    private fun clearAddContactForm() {
        showAddContacts.value = false
        phoneNumber.value = ""
        clearPhoneNumberError()
    }

    private fun clearPhoneNumberError() {
        actionState.update { state -> state.copy(phoneNumberError = null) }
    }

    private fun unblock(contactId: String) {
        updateContact(contactId) {
            unblockContact(contactId)
        }
    }

    private fun updateContact(
        contactId: String,
        operation: suspend () -> Result<Unit>
    ) {
        if (actionState.value.processingContactId != null) return

        viewModelScope.launch {
            actionState.update { it.copy(processingContactId = contactId) }

            operation()
                .onSuccess {
                    clearAddContactForm()
                }.onFailure { error ->
                    _effects.send(
                        BlockedContactsEffect.ShowError(
                            message = error.message ?: "Blocked contacts could not be updated"
                        )
                    )
                }

            actionState.update { it.copy(processingContactId = null) }
        }
    }

    private data class BlockedContactsFormState(
        val showAddContacts: Boolean = false,
        val phoneNumber: String = ""
    )

    private data class BlockedContactsActionState(
        val phoneNumberError: String? = null,
        val processingContactId: String? = null
    )

    private companion object {
        const val SHOW_ADD_CONTACTS_KEY = "showAddContacts"
        const val PHONE_NUMBER_KEY = "phoneNumber"
        const val PHONE_NUMBER_OPERATION_ID = "phone-number"
    }
}
