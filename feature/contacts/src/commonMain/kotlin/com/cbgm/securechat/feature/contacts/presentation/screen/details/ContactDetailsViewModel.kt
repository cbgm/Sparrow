package com.cbgm.securechat.feature.contacts.presentation.screen.details

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContactSafetyNumber
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContact
import com.cbgm.securechat.feature.contacts.domain.usecase.VerifyContact
import com.cbgm.securechat.feature.contacts.presentation.model.ContactDetailsUiEvent
import com.cbgm.securechat.feature.contacts.presentation.model.ContactDetailsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class ContactDetailsViewModel(
    private val contactId: String,
    private val getContact: GetContact,
    private val observeContact: ObserveContact,
    private val getContactSafetyNumber: GetContactSafetyNumber,
    private val verifyContact: VerifyContact
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<ContactDetailsUiState>(ContactDetailsUiState.Loading)

    val uiState: StateFlow<ContactDetailsUiState> = _uiState.asStateFlow()

    init {
        loadContact()
        observeContactChanges()
    }

    fun onUiEvent(event: ContactDetailsUiEvent) {
        when (event) {
            ContactDetailsUiEvent.BackClicked -> navigator.popBackStack()
            ContactDetailsUiEvent.RetryClicked -> loadContact()
            ContactDetailsUiEvent.ConfirmVerificationClicked -> confirmVerification()
            ContactDetailsUiEvent.ScanQrCodeClicked -> scanQrCode()
            ContactDetailsUiEvent.ShareContactClicked,
            ContactDetailsUiEvent.VerifyIdentityClicked,
            ContactDetailsUiEvent.VerificationBackClicked -> Unit
        }
    }

    private fun observeContactChanges() {
        viewModelScope.launch {
            observeContact(contactId)
                .drop(1)
                .collect { loadContact() }
        }
    }

    private fun scanQrCode() {
        navigator.navigateTo(AppRoute.VerifyIdentityQr(contactId = contactId))
    }

    private fun loadContact() {
        viewModelScope.launch {
            _uiState.value = ContactDetailsUiState.Loading

            val contactResult = getContact(contactId = contactId)

            val contact =
                contactResult.getOrElse { error ->
                    _uiState.value =
                        ContactDetailsUiState.Error(
                            message = error.message ?: "Failed to load contact"
                        )

                    return@launch
                }

            if (contact == null) {
                _uiState.value = ContactDetailsUiState.NotFound

                return@launch
            }

            val remoteIdentity = contact.secureChatIdentity

            if (remoteIdentity == null) {
                _uiState.value =
                    ContactDetailsUiState.Content(
                        contact = contact,
                        safetyNumber = null
                    )

                return@launch
            }

            val safetyNumber =
                getContactSafetyNumber
                    .invoke(contactId = contactId)
                    .getOrElse { error ->
                        _uiState.value =
                            ContactDetailsUiState.Error(
                                message = error.message ?: "Failed to generate safety number"
                            )

                        return@launch
                    }

            _uiState.value =
                ContactDetailsUiState.Content(
                    contact = contact,
                    safetyNumber = safetyNumber
                )
        }
    }

    private fun confirmVerification() {
        val current = _uiState.value as? ContactDetailsUiState.Content ?: return

        if (current.contact.secureChatIdentity?.verificationStatus == ContactVerificationStatus.VERIFIED) {
            return
        }

        if (!current.canVerify || current.isSavingVerification) {
            return
        }

        _uiState.value = current.copy(isSavingVerification = true, verificationError = null)

        viewModelScope.launch {
            verifyContact(contactId = contactId)
                .onSuccess { verifiedContact ->
                    val latest = _uiState.value as? ContactDetailsUiState.Content

                    if (latest != null) {
                        _uiState.value =
                            latest.copy(
                                contact = verifiedContact,
                                isSavingVerification = false,
                                verificationError = null
                            )
                    }
                }.onFailure { error ->
                    val latest = _uiState.value as? ContactDetailsUiState.Content

                    if (latest != null) {
                        _uiState.value =
                            latest.copy(
                                isSavingVerification = false,
                                verificationError = error.message ?: "Failed to verify identity"
                            )
                    }
                }
        }
    }
}
