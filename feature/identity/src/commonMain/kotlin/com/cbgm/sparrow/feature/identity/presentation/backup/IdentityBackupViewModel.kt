package com.cbgm.sparrow.feature.identity.presentation.backup

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityBackupStatusUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetPublicIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.MarkIdentityBackupExportedUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.PrepareIdentityBackupUseCase
import com.cbgm.sparrow.feature.identity.presentation.backup.mapper.toUi
import com.cbgm.sparrow.feature.identity.presentation.backup.model.BackupStateUi
import com.cbgm.sparrow.feature.identity.presentation.backup.model.IdentityBackupUiEvent
import com.cbgm.sparrow.feature.identity.presentation.backup.model.IdentityBackupUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_identity_backup_exported
import com.cbgm.sparrow.resources.feature_identity_backup_failed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class IdentityBackupViewModel(
    private val prepareIdentityBackup: PrepareIdentityBackupUseCase,
    private val markIdentityBackupExported: MarkIdentityBackupExportedUseCase,
    private val getIdentityBackupStatus: GetIdentityBackupStatusUseCase,
    private val getPublicIdentity: GetPublicIdentityUseCase
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
            is IdentityBackupUiEvent.PrepareBackup -> prepareBackup(event.password)
            is IdentityBackupUiEvent.BackupFileWritten -> backupFileWritten(event.success)
        }
    }

    private fun refreshBackupStatus() {
        viewModelScope.launch {
            getIdentityBackupStatus()
                .onSuccess { status ->
                    _uiState.value = _uiState.value.copy(status = status.toUi())
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(status = BackupStateUi.NOT_BACKED_UP)
                }
        }
    }

    private fun prepareBackup(password: CharArray) {
        if (_uiState.value.busy || _exportDocument.value != null) {
            password.fill('\u0000')
            return
        }
        _uiState.value = _uiState.value.copy(busy = true, message = null)
        viewModelScope.launch {
            try {
                val publicIdentity = getPublicIdentity().getOrNull()
                if (publicIdentity == null) {
                    setBackupResult(isError = true)
                    password.fill('\u0000')
                    return@launch
                }
                pendingSigningPublicKey = publicIdentity.signingPublicKey.copyOf()
                pendingEncryptionPublicKey = publicIdentity.encryptionPublicKey.copyOf()

                prepareIdentityBackup(password)
                    .onSuccess { _exportDocument.value = it }
                    .onFailure {
                        pendingSigningPublicKey = null
                        pendingEncryptionPublicKey = null
                        setBackupResult(isError = true)
                    }
            } finally {
                password.fill('\u0000')
            }
        }
    }

    private fun backupFileWritten(success: Boolean) {
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
                        setBackupResult(isError = false)
                    }
                    .onFailure {
                        setBackupResult(isError = true)
                    }
            } else {
                setBackupResult(isError = true)
            }
        }
    }

    private suspend fun setBackupResult(isError: Boolean) {
        _uiState.value = _uiState.value.copy(
            busy = false,
            message = if (isError) null else getString(Res.string.feature_identity_backup_exported),
            error = false
        )
        if (isError) SparrowLog.showError(getString(Res.string.feature_identity_backup_failed))
    }
}
