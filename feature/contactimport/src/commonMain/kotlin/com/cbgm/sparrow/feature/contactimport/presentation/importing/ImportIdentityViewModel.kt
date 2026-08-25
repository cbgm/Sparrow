package com.cbgm.sparrow.feature.contactimport.presentation.importing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.extensions.toFingerprint
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.contactimport.domain.usecase.ImportSharedIdentityUseCase
import com.cbgm.sparrow.feature.contactimport.presentation.importing.model.ImportIdentityUiEvent
import com.cbgm.sparrow.feature.contactimport.presentation.importing.model.ImportIdentityUiState
import com.cbgm.sparrow.feature.contactimport.presentation.scan.model.ScannedIdentityPreview
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityImportTrust
import com.cbgm.sparrow.feature.identity.domain.usecase.DecodeSharedIdentityUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImportIdentityViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val decodeSharedIdentity: DecodeSharedIdentityUseCase,
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

    init {
        scannedIdentity
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let(::prepareScannedIdentity)
    }

    fun onUiEvent(event: ImportIdentityUiEvent) {
        when (event) {
            is ImportIdentityUiEvent.EncodedIdentityChanged -> updateEncodedIdentity(event.value)
            ImportIdentityUiEvent.ImportClicked -> importIdentity(IdentityImportTrust.UNVERIFIED)
            ImportIdentityUiEvent.ScannedIdentityConfirmed -> confirmScannedIdentity()
            ImportIdentityUiEvent.ScannedIdentityDismissed -> dismissScannedIdentity()
            ImportIdentityUiEvent.BackClicked -> navigator.popBackStack()
            ImportIdentityUiEvent.ScanQrCodeClicked -> scanQrCode()
        }
    }

    private fun prepareScannedIdentity(encodedIdentity: String) {
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
            }.onFailure {
                updateEncodedIdentity(encodedIdentity)
            }
    }

    private fun confirmScannedIdentity() {
        val preview = _uiState.value.scannedIdentityPreview ?: return
        updateEncodedIdentity(preview.encodedIdentity)
        _uiState.update { it.copy(scannedIdentityPreview = null) }
        importIdentity(IdentityImportTrust.VERIFIED_IN_PERSON)
    }

    private fun dismissScannedIdentity() {
        _uiState.update { it.copy(scannedIdentityPreview = null) }
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

    private fun importIdentity(identityImportTrust: IdentityImportTrust) {
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
                        encodedIdentity = "",
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
