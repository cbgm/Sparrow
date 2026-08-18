package com.cbgm.sparrow.feature.contacts.presentation.details

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactSafetyNumberUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactProfilePictureUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.VerifyContactUseCase
import com.cbgm.sparrow.feature.contacts.presentation.details.mapper.toUiState
import com.cbgm.sparrow.feature.contacts.presentation.details.mapper.withVerificationState
import com.cbgm.sparrow.feature.contacts.presentation.details.model.ContactDetailsUiEvent
import com.cbgm.sparrow.feature.contacts.presentation.details.model.ContactDetailsUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ContactDetailsViewModel(
    private val contactId: String,
    private val observeContact: ObserveContactUseCase,
    private val getContactSafetyNumber: GetContactSafetyNumberUseCase,
    private val verifyContact: VerifyContactUseCase,
    private val observeProfilePicture: ObserveContactProfilePictureUseCase
) : BaseViewModel() {
    private val reloadRevision = MutableStateFlow(0)
    private val verificationState = MutableStateFlow(VerificationActionState())

    val uiState: StateFlow<ContactDetailsUiState> =
        combine(
            observeContactDetails(),
            verificationState
        ) { details, verification ->
            details.withVerificationState(
                isSaving = verification.isSaving,
                errorMessage = verification.errorMessage
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ContactDetailsUiState.Loading
        )

    fun onUiEvent(event: ContactDetailsUiEvent) {
        when (event) {
            ContactDetailsUiEvent.BackClicked -> navigator.popBackStack()
            ContactDetailsUiEvent.RetryClicked -> reload()
            ContactDetailsUiEvent.ConfirmVerificationClicked -> confirmVerification()
            ContactDetailsUiEvent.ScanQrCodeClicked -> scanQrCode()
            ContactDetailsUiEvent.ShareContactClicked,
            ContactDetailsUiEvent.VerifyIdentityClicked,
            ContactDetailsUiEvent.VerificationBackClicked -> Unit
        }
    }

    private fun observeContactDetails(): Flow<ContactDetailsUiState> =
        reloadRevision.flatMapLatest {
            combine(
                observeContact(contactId),
                observeProfilePicture(contactId)
            ) { contact, profilePictureBytes ->
                loadContactDetailsState(contact, profilePictureBytes)
            }.onStart {
                emit(ContactDetailsUiState.Loading)
            }.catch { error ->
                emit(
                    ContactDetailsUiState.Error(
                        message = error.message ?: "Failed to load contact"
                    )
                )
            }
        }

    private suspend fun loadContactDetailsState(
        contact: Contact?,
        profilePictureBytes: ByteArray?
    ): ContactDetailsUiState {
        contact ?: return ContactDetailsUiState.NotFound

        val safetyNumber =
            if (contact.sparrowIdentity == null) {
                null
            } else {
                getContactSafetyNumber.invoke(contactId)
                    .getOrElse { error ->
                        return ContactDetailsUiState.Error(
                            message = error.message ?: "Failed to generate safety number"
                        )
                    }
            }

        return contact.toUiState(
            safetyNumber = safetyNumber,
            profilePictureBytes = profilePictureBytes
        )
    }

    private fun reload() {
        verificationState.value = VerificationActionState()
        reloadRevision.update { revision -> revision + 1 }
    }

    private fun scanQrCode() {
        navigator.navigateTo(AppRoute.VerifyIdentityQr(contactId = contactId))
    }

    private fun confirmVerification() {
        val current = uiState.value as? ContactDetailsUiState.Content ?: return
        if (current.contact.sparrowIdentity?.verificationStatus == ContactVerificationStatus.VERIFIED) return
        if (!current.canVerify || verificationState.value.isSaving) return

        verificationState.value = VerificationActionState(isSaving = true)

        viewModelScope.launch {
            verifyContact(contactId)
                .onSuccess {
                    verificationState.value = VerificationActionState()
                    reloadRevision.update { revision -> revision + 1 }
                }.onFailure { error ->
                    verificationState.value =
                        VerificationActionState(
                            errorMessage = error.message ?: "Failed to verify identity"
                        )
                }
        }
    }

    private data class VerificationActionState(
        val isSaving: Boolean = false,
        val errorMessage: String? = null
    )
}
