package com.cbgm.sparrow.feature.contactimport.presentation.importing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.contactimport.domain.usecase.ImportSharedIdentityUseCase
import com.cbgm.sparrow.feature.contactimport.presentation.importing.model.ImportIdentityUiEvent
import com.cbgm.sparrow.feature.contactimport.presentation.importing.model.ImportIdentityUiState
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityImportTrust
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImportIdentityViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val importSharedIdentity: ImportSharedIdentityUseCase
) : BaseViewModel() {
    private val contactId = savedStateHandle.get<String>(AppRoute.ImportContact::contactId.name)
    private val scannedIdentity = savedStateHandle.get<String>(AppRoute.ImportContact::scannedIdentity.name)
    private val _uiState =
        MutableStateFlow(
            ImportIdentityUiState(
                encodedIdentity = savedStateHandle.get<String>(ENCODED_IDENTITY_KEY).orEmpty()
            )
        )
    val uiState: StateFlow<ImportIdentityUiState> = _uiState.asStateFlow()

    fun onUiEvent(event: ImportIdentityUiEvent) {
        when (event) {
            is ImportIdentityUiEvent.EncodedIdentityChanged -> updateEncodedIdentity(event.value)
            is ImportIdentityUiEvent.ImportClicked -> importIdentity(event.contactId, event.identityImportTrust)
            ImportIdentityUiEvent.BackClicked -> navigator.popBackStack()
            ImportIdentityUiEvent.ScanQrCodeClicked -> scanQrCode()
        }
    }

    private fun scanQrCode() {
        navigator.navigateTo(
            AppRoute.ScanIdentity(
                contactId = contactId,
                previousScannedIdentity = scannedIdentity
            )
        )
    }

    private fun updateEncodedIdentity(value: String) {
        savedStateHandle[ENCODED_IDENTITY_KEY] = value
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
                it.copy(errorMessage = "Paste a shared Sparrow identity first")
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
                savedStateHandle[ENCODED_IDENTITY_KEY] = ""
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

    private companion object {
        const val ENCODED_IDENTITY_KEY = "encodedIdentity"
    }
}
