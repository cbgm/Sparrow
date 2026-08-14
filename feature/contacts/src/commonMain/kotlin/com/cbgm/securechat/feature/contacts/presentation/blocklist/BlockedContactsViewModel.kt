package com.cbgm.securechat.feature.contacts.presentation.blocklist

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.contacts.domain.usecase.BlockContactUseCase
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContactBlocklistUseCase
import com.cbgm.securechat.feature.contacts.domain.usecase.UnblockContactUseCase
import com.cbgm.securechat.feature.contacts.presentation.blocklist.model.BlockedContactsEffect
import com.cbgm.securechat.feature.contacts.presentation.blocklist.model.BlockedContactsUiEvent
import com.cbgm.securechat.feature.contacts.presentation.blocklist.model.BlockedContactsUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BlockedContactsViewModel(
    observeContactBlocklist: ObserveContactBlocklistUseCase,
    private val blockContact: BlockContactUseCase,
    private val unblockContact: UnblockContactUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(BlockedContactsUiState())
    val uiState: StateFlow<BlockedContactsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<BlockedContactsEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onUiEvent(event: BlockedContactsUiEvent) {
        when (event) {
            BlockedContactsUiEvent.BackClicked -> navigateBack()
            BlockedContactsUiEvent.AddContactClicked -> showAddContacts()
            BlockedContactsUiEvent.AddContactsDismissed -> dismissAddContacts()
            is BlockedContactsUiEvent.PhoneNumberChanged -> updatePhoneNumber(event.value)
            BlockedContactsUiEvent.BlockPhoneNumberClicked -> blockPhoneNumber()
            is BlockedContactsUiEvent.BlockContactClicked -> block(event.contactId)
            is BlockedContactsUiEvent.UnblockContactClicked -> unblock(event.contactId)
        }
    }

    private fun navigateBack() {
        navigator.popBackStack()
    }

    init {
        viewModelScope.launch {
            observeContactBlocklist().collect { blocklist ->
                _uiState.update {
                    it.copy(
                        blockedContacts = blocklist.blockedContacts,
                        availableContacts = blocklist.availableContacts
                    )
                }
            }
        }
    }

    private fun showAddContacts() {
        _uiState.update {
            it.copy(
                showAddContacts = true,
                phoneNumber = "",
                phoneNumberError = null
            )
        }
    }

    private fun dismissAddContacts() {
        _uiState.update {
            it.copy(
                showAddContacts = false,
                phoneNumber = "",
                phoneNumberError = null
            )
        }
    }

    private fun updatePhoneNumber(phoneNumber: String) {
        _uiState.update {
            it.copy(
                phoneNumber = phoneNumber,
                phoneNumberError = null
            )
        }
    }

    private fun block(contactId: String) {
        updateContact(contactId) {
            blockContact(contactId)
        }
    }

    private fun blockPhoneNumber() {
        val phoneNumber = _uiState.value.phoneNumber.trim()

        if (phoneNumber.isEmpty() || _uiState.value.processingContactId != null) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    processingContactId = PHONE_NUMBER_OPERATION_ID,
                    phoneNumberError = null
                )
            }

            blockContact
                .byPhoneNumber(phoneNumber)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            showAddContacts = false,
                            phoneNumber = "",
                            phoneNumberError = null
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            phoneNumberError = error.message ?: "Phone number could not be blocked"
                        )
                    }
                }

            _uiState.update { it.copy(processingContactId = null) }
        }
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
        if (_uiState.value.processingContactId != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(processingContactId = contactId) }

            operation()
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            showAddContacts = false,
                            phoneNumber = "",
                            phoneNumberError = null
                        )
                    }
                }.onFailure { error ->
                    _effects.send(
                        BlockedContactsEffect.ShowError(
                            message = error.message ?: "Blocked contacts could not be updated"
                        )
                    )
                }

            _uiState.update { it.copy(processingContactId = null) }
        }
    }

    private companion object {
        const val PHONE_NUMBER_OPERATION_ID = "phone-number"
    }
}
