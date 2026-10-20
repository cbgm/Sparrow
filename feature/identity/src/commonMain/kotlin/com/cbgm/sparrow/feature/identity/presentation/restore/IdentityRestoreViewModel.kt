package com.cbgm.sparrow.feature.identity.presentation.restore

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackup
import com.cbgm.sparrow.feature.identity.domain.usecase.NormalizeLocalPhoneNumberUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.restore.ReadIdentityBackupUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.restore.ReadIdentityRestoreDocumentUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.restore.SaveRestoredIdentityUseCase
import com.cbgm.sparrow.feature.identity.presentation.restore.model.IdentityRestoreStageUi
import com.cbgm.sparrow.feature.identity.presentation.restore.model.IdentityRestoreUiEvent
import com.cbgm.sparrow.feature.identity.presentation.restore.model.IdentityRestoreUiState
import com.cbgm.sparrow.feature.identity.presentation.verification.model.PhoneVerificationResultUi
import com.cbgm.sparrow.feature.media.presentation.filepicker.FilePickerLauncher
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerSessionResultUi
import com.cbgm.sparrow.feature.media.presentation.model.FileMediaSelectionUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_identity_restore_failed
import com.cbgm.sparrow.resources.feature_identity_restore_invalid_phone
import com.cbgm.sparrow.resources.feature_identity_restore_read_failed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/** Restore from a backup file, shown on top of the onboarding screen. */
class IdentityRestoreViewModel(
    private val filePicker: FilePickerLauncher,
    private val readDocument: ReadIdentityRestoreDocumentUseCase,
    private val normalizePhoneNumber: NormalizeLocalPhoneNumberUseCase,
    private val readBackup: ReadIdentityBackupUseCase,
    private val saveRestoredIdentity: SaveRestoredIdentityUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(IdentityRestoreUiState())
    val uiState = _uiState.asStateFlow()

    // The number waiting for SMS verification; the backup is stored once it is verified.
    private val _phoneToVerify = MutableStateFlow<String?>(null)
    val phoneToVerify = _phoneToVerify.asStateFlow()
    private var document: ByteArray? = null
    private var backup: IdentityBackup? = null
    private var phoneNumber = ""
    private var name = ""

    init {
        viewModelScope.launch {
            filePicker.results.collect {
                val result = filePicker.consumeResult() ?: return@collect
                if (_uiState.value.stage != IdentityRestoreStageUi.SELECTING) return@collect
                when (result) {
                    is FilePickerSessionResultUi.Completed -> readSelection(result)
                    is FilePickerSessionResultUi.Dismissed -> dismiss()
                    is FilePickerSessionResultUi.Failed -> report(Res.string.feature_identity_restore_read_failed)
                }
            }
        }
    }

    fun onUiEvent(event: IdentityRestoreUiEvent) {
        when (event) {
            is IdentityRestoreUiEvent.PasswordChanged -> {
                if (_uiState.value.stage == IdentityRestoreStageUi.PASSWORD) {
                    _uiState.update { it.copy(password = event.value) }
                }
            }
            IdentityRestoreUiEvent.ConfirmClicked -> confirm()
            IdentityRestoreUiEvent.DismissClicked -> {
                if (_uiState.value.stage == IdentityRestoreStageUi.PASSWORD) dismiss()
            }
        }
    }

    /** Starts with the number and name entered on the phone page. */
    fun start(phoneNumber: String, name: String) {
        if (_uiState.value.stage != IdentityRestoreStageUi.IDLE) return
        this@IdentityRestoreViewModel.phoneNumber = normalizePhoneNumber(phoneNumber).getOrElse {
            dismiss()
            viewModelScope.launch { report(Res.string.feature_identity_restore_invalid_phone) }
            return
        }
        this@IdentityRestoreViewModel.name = name
        _uiState.value = IdentityRestoreUiState(stage = IdentityRestoreStageUi.SELECTING)
        val sessionId = filePicker.launch(
            maxItems = 1,
            maxFileBytes = ReadIdentityRestoreDocumentUseCase.MAX_DOCUMENT_BYTES,
            blockedSourceReferences = emptySet()
        )
        navigator.navigateTo(AppRoute.FilePicker(sessionId))
    }

    private suspend fun readSelection(result: FilePickerSessionResultUi.Completed) {
        val file = result.media.singleOrNull() as? FileMediaSelectionUi
        if (file == null) {
            dismiss()
            report(Res.string.feature_identity_restore_read_failed)
            return
        }
        _uiState.value = IdentityRestoreUiState(stage = IdentityRestoreStageUi.READING)
        readDocument(file.localFilePath, file.byteSize).onSuccess {
            document = it
            _uiState.value = IdentityRestoreUiState(stage = IdentityRestoreStageUi.PASSWORD)
        }.onFailure {
            dismiss()
            report(Res.string.feature_identity_restore_read_failed)
        }
    }

    // The backup is only decrypted and checked here; it is stored after the phone was verified.
    private fun confirm() {
        val state = _uiState.value
        if (!state.canConfirm) return
        val document = document ?: return
        val password = state.password.toCharArray()
        _uiState.value = IdentityRestoreUiState(stage = IdentityRestoreStageUi.RESTORING)
        viewModelScope.launch {
            val read =
                try {
                    readBackup(document, password)
                } finally {
                    password.fill('\u0000')
                }
            read.onSuccess {
                document.fill(0)
                this@IdentityRestoreViewModel.document = null
                backup = it
                _uiState.value = IdentityRestoreUiState(stage = IdentityRestoreStageUi.VERIFYING)
                _phoneToVerify.value = phoneNumber
            }.onFailure {
                _uiState.value = IdentityRestoreUiState(stage = IdentityRestoreStageUi.PASSWORD)
                report(Res.string.feature_identity_restore_failed)
            }
        }
    }

    fun onPhoneVerification(result: PhoneVerificationResultUi) {
        _phoneToVerify.value = null
        if (_uiState.value.stage != IdentityRestoreStageUi.VERIFYING) return
        when (result) {
            PhoneVerificationResultUi.Verified -> viewModelScope.launch { restore() }
            PhoneVerificationResultUi.Cancelled -> dismiss()
        }
    }

    private suspend fun restore() {
        val backup = backup ?: return
        this.backup = null
        saveRestoredIdentity(backup, phoneNumber, name).onSuccess {
            clearSensitiveData()
            _uiState.value = IdentityRestoreUiState(stage = IdentityRestoreStageUi.COMPLETED)
        }.onFailure {
            dismiss()
            report(Res.string.feature_identity_restore_failed)
        }
    }

    /** Back to idle after the restore completed or was dismissed. */
    fun finish() {
        val stage = _uiState.value.stage
        if (stage != IdentityRestoreStageUi.COMPLETED && stage != IdentityRestoreStageUi.DISMISSED) return
        reset()
    }

    private fun dismiss() {
        clearSensitiveData()
        _uiState.value = IdentityRestoreUiState(stage = IdentityRestoreStageUi.DISMISSED)
    }

    private suspend fun report(resource: StringResource) {
        SparrowLog.showError(getString(resource))
    }

    private fun clearSensitiveData() {
        document?.fill(0)
        document = null
        backup?.wipe()
        backup = null
        phoneNumber = ""
        name = ""
    }

    private fun reset() {
        clearSensitiveData()
        _uiState.value = IdentityRestoreUiState()
    }

    override fun onCleared() {
        reset()
    }
}
