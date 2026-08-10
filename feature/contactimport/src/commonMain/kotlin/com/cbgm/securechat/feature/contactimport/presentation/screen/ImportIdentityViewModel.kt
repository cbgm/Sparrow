package com.cbgm.securechat.feature.contactimport.presentation.screen

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.contactimport.domain.usecase.ImportSharedIdentity
import com.cbgm.securechat.feature.contactimport.presentation.model.ImportIdentityUiEvent
import com.cbgm.securechat.feature.contactimport.presentation.model.ImportIdentityUiState
import com.cbgm.securechat.feature.contacts.domain.model.IdentityImportTrust
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImportIdentityViewModel(
    private val importSharedIdentity: ImportSharedIdentity
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(ImportIdentityUiState())
    val uiState: StateFlow<ImportIdentityUiState> = _uiState.asStateFlow()

    private fun scanQrCode() {
        navigator.navigateTo(AppRoute.ScanIdentity)
    }

    private fun navigateBack() {
        navigator.popBackStack()
    }

    fun onUiEvent(event: ImportIdentityUiEvent) {
        when (event) {
            is ImportIdentityUiEvent.EncodedIdentityChanged -> updateEncodedIdentity(event.value)
            is ImportIdentityUiEvent.ImportClicked -> importIdentity(event.contactId, event.identityImportTrust)
            ImportIdentityUiEvent.BackClicked -> navigateBack()
            ImportIdentityUiEvent.ScanQrCodeClicked -> scanQrCode()
        }
    }

    private fun updateEncodedIdentity(value: String) {
        _uiState.update {
            it.copy(
                encodedIdentity = value,
                importedContactName = null,
                importedIdentityTrust = null,
                errorMessage = null
            )
        }
    }

    private fun importIdentity(
        contactId: String?,
        identityImportTrust: IdentityImportTrust
    ) {
        val encodedIdentity = _uiState.value.encodedIdentity.trim()

        if (encodedIdentity.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Paste a shared SecureChat identity first")
            }
            return
        }

        if (_uiState.value.isImporting) return

        _uiState.update {
            it.copy(
                isImporting = true,
                importedContactName = null,
                importedIdentityTrust = null,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            importSharedIdentity(
                encodedIdentity = encodedIdentity,
                contactId = contactId,
                identityImportTrust = identityImportTrust
            ).onSuccess { contact ->
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importedContactName = contact.displayName ?: "Unnamed contact",
                        importedIdentityTrust = identityImportTrust,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importedContactName = null,
                        importedIdentityTrust = null,
                        errorMessage = error.message ?: "Identity import failed"
                    )
                }
            }
        }
    }
}
