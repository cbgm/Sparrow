package com.cbgm.securechat.feature.contactimport.presentation.screen

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.contactimport.domain.usecase.VerifyContactByQr
import com.cbgm.securechat.feature.contactimport.presentation.model.VerifyContactQrUiEvent
import com.cbgm.securechat.feature.contactimport.presentation.model.VerifyContactQrUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VerifyContactQrViewModel(
    private val contactId: String,
    private val verifyContactByQr: VerifyContactByQr
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(VerifyContactQrUiState())
    val uiState: StateFlow<VerifyContactQrUiState> = _uiState.asStateFlow()

    fun onUiEvent(event: VerifyContactQrUiEvent) {
        when (event) {
            is VerifyContactQrUiEvent.QrCodeScanned -> onQrCodeScanned(event.encodedIdentity)
            VerifyContactQrUiEvent.BackClicked -> navigator.popBackStack()
            VerifyContactQrUiEvent.ErrorDismissed -> dismissError()
        }
    }

    private fun onQrCodeScanned(encodedIdentity: String) {
        if (_uiState.value.isVerifying || _uiState.value.isVerified) {
            return
        }

        _uiState.update {
            it.copy(
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

    private fun dismissError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }
}
