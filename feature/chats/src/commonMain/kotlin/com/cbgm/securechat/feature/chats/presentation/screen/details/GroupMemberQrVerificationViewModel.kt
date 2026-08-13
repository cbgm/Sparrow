package com.cbgm.securechat.feature.chats.presentation.screen.details

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.extensions.toFingerprint
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.chats.domain.usecase.VerifyGroupMember
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberQrVerificationError
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberQrVerificationUiEvent
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberQrVerificationUiState
import com.cbgm.securechat.feature.contactimport.presentation.scan.model.ScannedIdentityPreview
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import com.cbgm.securechat.feature.identity.domain.model.SharedIdentityPayload
import com.cbgm.securechat.feature.identity.domain.usecase.DecodeSharedIdentity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupMemberQrVerificationViewModel(
    private val groupId: String,
    private val contactId: String,
    private val decodeSharedIdentity: DecodeSharedIdentity,
    private val getContact: GetContact,
    private val verifyGroupMember: VerifyGroupMember
) : BaseViewModel() {
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
                ?.secureChatIdentity

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

private fun SharedIdentityPayload.toPreview(encodedIdentity: String): ScannedIdentityPreview =
    ScannedIdentityPreview(
        encodedIdentity = encodedIdentity,
        displayName = contactDetails.displayName,
        phoneNumber = contactDetails.phoneNumber,
        signingKeyFingerprint = signingPublicKey.toFingerprint(),
        encryptionKeyFingerprint = encryptionPublicKey.toFingerprint()
    )
