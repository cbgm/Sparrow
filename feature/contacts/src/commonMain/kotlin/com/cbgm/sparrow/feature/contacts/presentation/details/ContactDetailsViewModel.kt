package com.cbgm.sparrow.feature.contacts.presentation.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactDetailsContextUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.VerifyContactUseCase
import com.cbgm.sparrow.feature.contacts.presentation.details.mapper.toContactDetailsUiState
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ContactDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val observeContactDetailsContext: ObserveContactDetailsContextUseCase,
    private val verifyContact: VerifyContactUseCase
) : BaseViewModel() {
    private val contactId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.ContactDetails::contactId.name)
    private val conversationId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.ContactDetails::conversationId.name)
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
            ContactDetailsUiEvent.MediaAndFilesClicked ->
                navigator.navigateTo(AppRoute.AttachmentManagement(conversationId))
            ContactDetailsUiEvent.ShareContactClicked,
            ContactDetailsUiEvent.VerifyIdentityClicked,
            ContactDetailsUiEvent.VerificationBackClicked -> Unit
        }
    }

    private fun observeContactDetails(): Flow<ContactDetailsUiState> =
        reloadRevision.flatMapLatest {
            observeContactDetailsContext(contactId)
                .map { context ->
                    context.contact?.toContactDetailsUiState(
                        safetyNumber = context.safetyNumber,
                        profilePictureBytes = context.profilePictureBytes
                    ) ?: ContactDetailsUiState.NotFound
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
