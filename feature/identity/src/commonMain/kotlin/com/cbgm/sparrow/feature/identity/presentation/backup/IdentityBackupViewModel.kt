package com.cbgm.sparrow.feature.identity.presentation.backup

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackupStatus
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityBackupStatusUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetLocalIdentityNameUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetLocalPhoneNumberUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetPublicIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.MarkIdentityBackupExportedUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.NormalizeLocalPhoneNumberUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.PrepareIdentityBackupUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.RestoreIdentityBackupUseCase
import com.cbgm.sparrow.feature.identity.presentation.backup.model.IdentityBackupUiEvent
import com.cbgm.sparrow.feature.identity.presentation.backup.model.IdentityBackupUiState
import com.cbgm.sparrow.feature.identity.presentation.backup.model.IdentityBackupUiStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IdentityBackupViewModel(
    private val prepareIdentityBackup: PrepareIdentityBackupUseCase,
    private val restoreIdentityBackup: RestoreIdentityBackupUseCase,
    private val markIdentityBackupExported: MarkIdentityBackupExportedUseCase,
    private val getIdentityBackupStatus: GetIdentityBackupStatusUseCase,
    private val getPublicIdentity: GetPublicIdentityUseCase,
    private val normalizeLocalPhoneNumber: NormalizeLocalPhoneNumberUseCase,
    private val getLocalPhoneNumber: GetLocalPhoneNumberUseCase,
    private val getLocalIdentityName: GetLocalIdentityNameUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(IdentityBackupUiState())
    val uiState: StateFlow<IdentityBackupUiState> = _uiState.asStateFlow()

    private val _exportDocument = MutableStateFlow<ByteArray?>(null)
    val exportDocument: StateFlow<ByteArray?> = _exportDocument.asStateFlow()

    private var pendingSigningPublicKey: ByteArray? = null
    private var pendingEncryptionPublicKey: ByteArray? = null

    init {
        refreshBackupStatus()
    }

    fun onUiEvent(event: IdentityBackupUiEvent) {
        when (event) {
            IdentityBackupUiEvent.BackClicked -> navigator.popBackStack()
            IdentityBackupUiEvent.ExportIdentityClicked -> Unit
            IdentityBackupUiEvent.RestoreIdentityClicked -> Unit
            is IdentityBackupUiEvent.PrepareBackup -> prepareBackup(event.password)
            is IdentityBackupUiEvent.RestoreBackup -> restoreBackup(event.document, event.password)
            is IdentityBackupUiEvent.BackupFileWritten -> backupFileWritten(event.success, event.failure)
        }
    }

    private fun refreshBackupStatus() {
        viewModelScope.launch {
            getIdentityBackupStatus()
                .onSuccess { status ->
                    val uiStatus = when (status) {
                        IdentityBackupStatus.NOT_BACKED_UP -> IdentityBackupUiStatus.NOT_BACKED_UP
                        IdentityBackupStatus.EXPORTED -> IdentityBackupUiStatus.EXPORTED
                        IdentityBackupStatus.IMPORTED -> IdentityBackupUiStatus.IMPORTED
                    }
                    _uiState.value = _uiState.value.copy(status = uiStatus)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(status = IdentityBackupUiStatus.NOT_BACKED_UP)
                }
        }
    }

    private fun prepareBackup(password: CharArray) {
        if (_uiState.value.busy || _exportDocument.value != null) {
            password.fill('\u0000')
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, message = null)
            try {
                val publicIdentity = getPublicIdentity().getOrNull()
                if (publicIdentity == null) {
                    setBackupResult("Public identity not found", isError = true)
                    password.fill('\u0000')
                    return@launch
                }
                pendingSigningPublicKey = publicIdentity.signingPublicKey.copyOf()
                pendingEncryptionPublicKey = publicIdentity.encryptionPublicKey.copyOf()

                prepareIdentityBackup(password)
                    .onSuccess { _exportDocument.value = it }
                    .onFailure { error ->
                        pendingSigningPublicKey = null
                        pendingEncryptionPublicKey = null
                        setBackupResult(error.message ?: "Backup encryption failed", isError = true)
                    }
            } finally {
                password.fill('\u0000')
            }
        }
    }

    private fun backupFileWritten(success: Boolean, failure: String?) {
        val bytes = _exportDocument.value ?: return
        _exportDocument.value = null
        bytes.fill(0)
        val expectedSigning = pendingSigningPublicKey
        val expectedEncryption = pendingEncryptionPublicKey
        pendingSigningPublicKey = null
        pendingEncryptionPublicKey = null
        viewModelScope.launch {
            if (success && expectedSigning != null && expectedEncryption != null) {
                markIdentityBackupExported(expectedSigning, expectedEncryption)
                    .onSuccess {
                        refreshBackupStatus()
                        setBackupResult("Identity backup exported. Keep the file and password safe.", isError = false)
                    }
                    .onFailure { error ->
                        setBackupResult(error.message ?: "Failed to update backup status", isError = true)
                    }
            } else {
                setBackupResult(failure ?: "Backup export cancelled or failed", isError = true)
            }
        }
    }

    private fun restoreBackup(document: ByteArray, password: CharArray) {
        if (_uiState.value.busy) {
            document.fill(0)
            password.fill('\u0000')
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, message = null)
            try {
                val phone = getLocalPhoneNumber().getOrNull() ?: ""
                val normalized = normalizeLocalPhoneNumber(phone).getOrDefault(phone)
                val name = getLocalIdentityName().getOrNull() ?: ""
                restoreIdentityBackup(document, password, normalized, name)
                    .onSuccess {
                        refreshBackupStatus()
                        setBackupResult("Identity restored successfully", isError = false)
                    }
                    .onFailure { error ->
                        setBackupResult(error.message ?: "Identity restoration failed", isError = true)
                    }
            } finally {
                document.fill(0)
                password.fill('\u0000')
            }
        }
    }

    private fun setBackupResult(message: String, isError: Boolean) {
        _uiState.value = _uiState.value.copy(
            busy = false,
            message = message,
            error = isError
        )
    }

    fun showBackupError(message: String) {
        setBackupResult(message, isError = true)
    }
}
