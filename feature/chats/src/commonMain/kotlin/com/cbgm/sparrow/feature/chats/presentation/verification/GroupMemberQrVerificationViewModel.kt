package com.cbgm.sparrow.feature.chats.presentation.verification

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.chats.domain.usecase.group.VerifyGroupMemberUseCase
import com.cbgm.sparrow.feature.chats.presentation.verification.mapper.toPreview
import com.cbgm.sparrow.feature.chats.presentation.verification.model.GroupMemberQrVerificationError
import com.cbgm.sparrow.feature.chats.presentation.verification.model.GroupMemberQrVerificationUiEvent
import com.cbgm.sparrow.feature.chats.presentation.verification.model.GroupMemberQrVerificationUiState
import com.cbgm.sparrow.feature.contactimport.presentation.scan.model.ScannedIdentityPreview
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.DecodeSharedIdentityUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupMemberQrVerificationViewModel(
    savedStateHandle: SavedStateHandle,
    private val decodeSharedIdentity: DecodeSharedIdentityUseCase,
    private val getContact: GetContactUseCase,
    private val verifyGroupMember: VerifyGroupMemberUseCase
) : BaseViewModel() {
    private val groupId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.VerifyIdentityQr::groupId.name)
    private val contactId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.VerifyIdentityQr::contactId.name)
    private val _uiState = MutableStateFlow(GroupMemberQrVerificationUiState())
    val uiState: StateFlow<GroupMemberQrVerificationUiState> = _uiState.asStateFlow()

    fun onUiEvent(event: GroupMemberQrVerificationUiEvent) {
        when (event) {
            is GroupMemberQrVerificationUiEvent.QrCodeScanned -> scan(event.encodedIdentity)
            GroupMemberQrVerificationUiEvent.BackClicked -> navigator.popBackStack()
            GroupMemberQrVerificationUiEvent.ConfirmClicked -> confirm()
            GroupMemberQrVerificationUiEvent.PreviewDismissed -> dismissPreview()
            GroupMemberQrVerificationUiEvent.RetryClicked -> retry()
        }
    }

    private fun scan(encodedIdentity: String) {
        if (_uiState.value.isProcessing || _uiState.value.preview != null) {
            return
        }

        _uiState.update { state ->
            state.copy(
                isProcessing = true,
                error = null
            )
        }

        viewModelScope.launch {
            val preview = validateIdentity(encodedIdentity)
            _uiState.update { state ->
                state.copy(
                    preview = preview,
                    isProcessing = false
                )
            }
        }
    }

    private fun confirm() {
        val preview = _uiState.value.preview ?: return

        _uiState.update { state ->
            state.copy(
                preview = null,
                isProcessing = true,
                error = null
            )
        }

        viewModelScope.launch {
            if (validateIdentity(preview.encodedIdentity) == null) {
                _uiState.update { state -> state.copy(isProcessing = false) }
                return@launch
            }

            verifyGroupMember(
                groupId = groupId,
                contactId = contactId
            ).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        isProcessing = false,
                        isVerified = true
                    )
                }
                navigator.popBackStack()
            }.onFailure {
                _uiState.update { state ->
                    state.copy(
                        isProcessing = false,
                        error = GroupMemberQrVerificationError.VERIFICATION_FAILED
                    )
                }
            }
        }
    }

    private fun dismissPreview() {
        _uiState.update { state ->
            state.copy(
                preview = null,
                scanAttempt = state.scanAttempt + 1
            )
        }
    }

    private fun retry() {
        _uiState.update { state ->
            state.copy(
                error = null,
                scanAttempt = state.scanAttempt + 1
            )
        }
    }

    private suspend fun validateIdentity(encodedIdentity: String): ScannedIdentityPreview? {
        val scannedIdentity =
            decodeSharedIdentity(encodedIdentity)
                .getOrElse {
                    setError(GroupMemberQrVerificationError.INVALID_QR)
                    return null
                }
        val expectedIdentity =
            getContact(contactId)
                .getOrNull()
                ?.sparrowIdentity

        if (
            expectedIdentity == null ||
            !expectedIdentity.signingPublicKey.contentEquals(scannedIdentity.signingPublicKey) ||
            !expectedIdentity.encryptionPublicKey.contentEquals(scannedIdentity.encryptionPublicKey)
        ) {
            setError(GroupMemberQrVerificationError.IDENTITY_MISMATCH)
            return null
        }

        return scannedIdentity.toPreview(encodedIdentity)
    }

    private fun setError(error: GroupMemberQrVerificationError) {
        _uiState.update { state -> state.copy(error = error) }
    }
}
