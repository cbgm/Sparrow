package com.cbgm.sparrow.feature.contactimport.presentation.verify

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.extensions.toFingerprint
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.contactimport.domain.usecase.VerifyContactByQrUseCase
import com.cbgm.sparrow.feature.contactimport.presentation.scan.model.ScannedIdentityPreview
import com.cbgm.sparrow.feature.contactimport.presentation.verify.model.VerifyContactQrUiEvent
import com.cbgm.sparrow.feature.contactimport.presentation.verify.model.VerifyContactQrUiState
import com.cbgm.sparrow.feature.identity.domain.usecase.DecodeSharedIdentityUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VerifyContactQrViewModel(
    savedStateHandle: SavedStateHandle,
    private val decodeSharedIdentity: DecodeSharedIdentityUseCase,
    private val verifyContactByQr: VerifyContactByQrUseCase
) : BaseViewModel() {
    private val contactId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.VerifyIdentityQr::contactId.name)

    private val _uiState = MutableStateFlow(VerifyContactQrUiState())
    val uiState: StateFlow<VerifyContactQrUiState> = _uiState.asStateFlow()

    fun onUiEvent(event: VerifyContactQrUiEvent) {
        when (event) {
            is VerifyContactQrUiEvent.QrCodeScanned -> prepareScannedIdentity(event.encodedIdentity)
            VerifyContactQrUiEvent.ScannedIdentityConfirmed -> verifyScannedIdentity()
            VerifyContactQrUiEvent.ScannedIdentityDismissed -> dismissScannedIdentity()
            VerifyContactQrUiEvent.BackClicked -> navigator.popBackStack()
            VerifyContactQrUiEvent.ErrorDismissed -> dismissError()
        }
    }

    private fun prepareScannedIdentity(encodedIdentity: String) {
        if (_uiState.value.isVerifying || _uiState.value.isVerified) return

        decodeSharedIdentity(encodedIdentity)
            .onSuccess { payload ->
                _uiState.update {
                    it.copy(
                        scannedIdentityPreview =
                            ScannedIdentityPreview(
                                encodedIdentity = encodedIdentity,
                                displayName = payload.contactDetails.displayName,
                                phoneNumber = payload.contactDetails.phoneNumber,
                                signingKeyFingerprint = payload.signingPublicKey.toFingerprint(),
                                encryptionKeyFingerprint = payload.encryptionPublicKey.toFingerprint()
                            ),
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        scannedIdentityPreview = null,
                        errorMessage = error.message ?: "Invalid identity QR code"
                    )
                }
            }
    }

    private fun verifyScannedIdentity() {
        val encodedIdentity = _uiState.value.scannedIdentityPreview?.encodedIdentity ?: return

        _uiState.update {
            it.copy(
                scannedIdentityPreview = null,
                isVerifying = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            verifyContactByQr(
                contactId = contactId,
                encodedIdentity = encodedIdentity
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        isVerifying = false,
                        isVerified = true,
                        errorMessage = null
                    )
                }
                navigator.popBackStack()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isVerifying = false,
                        isVerified = false,
                        errorMessage = error.message ?: "Identity QR code could not be verified"
                    )
                }
            }
        }
    }

    private fun dismissScannedIdentity() {
        _uiState.update { it.copy(scannedIdentityPreview = null) }
    }

    private fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
