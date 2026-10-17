package com.cbgm.sparrow.feature.identity.presentation.restore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.identity.domain.usecase.NormalizeLocalPhoneNumberUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.restore.ReadIdentityRestoreDocumentUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.restore.RestoreIdentityBackupUseCase
import com.cbgm.sparrow.feature.identity.presentation.restore.model.IdentityRestoreStageUi
import com.cbgm.sparrow.feature.identity.presentation.restore.model.IdentityRestoreUiEvent
import com.cbgm.sparrow.feature.identity.presentation.restore.model.IdentityRestoreUiState
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

class IdentityRestoreViewModel(
    savedStateHandle: SavedStateHandle,
    private val filePicker: FilePickerLauncher,
    private val readDocument: ReadIdentityRestoreDocumentUseCase,
    private val normalizePhoneNumber: NormalizeLocalPhoneNumberUseCase,
    private val restoreIdentityBackup: RestoreIdentityBackupUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(IdentityRestoreUiState())
    val uiState = _uiState.asStateFlow()
    val verificationPhoneNumber: String?
        get() = if (_uiState.value.phoneVerificationPending) phoneNumber else null
    private var document: ByteArray? = null
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
        val phoneNumber = savedStateHandle.get<String>(AppRoute.IdentityRestore::phoneNumber.name)
        val name = savedStateHandle.get<String>(AppRoute.IdentityRestore::name.name)
        if (phoneNumber == null || name == null) {
            dismiss()
            viewModelScope.launch { report(Res.string.feature_identity_restore_invalid_phone) }
        } else {
            open(phoneNumber, name)
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

    private fun open(phoneNumber: String, name: String) {
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

    private fun confirm() {
        val state = _uiState.value
        if (!state.canConfirm) return
        _uiState.update { it.copy(phoneVerificationPending = true) }
    }

    fun onVerificationCancelled() {
        _uiState.update { it.copy(phoneVerificationPending = false) }
    }

    fun onVerificationContinued() {
        val state = _uiState.value
        if (!state.phoneVerificationPending || !state.canConfirm) return
        val document = document ?: return
        val password = state.password.toCharArray()
        _uiState.value = IdentityRestoreUiState(stage = IdentityRestoreStageUi.RESTORING)
        viewModelScope.launch {
            try {
                restoreIdentityBackup(document, password, phoneNumber, name).onSuccess {
                    clearSensitiveData()
                    _uiState.value = IdentityRestoreUiState(stage = IdentityRestoreStageUi.COMPLETED)
                }.onFailure {
                    _uiState.value = IdentityRestoreUiState(stage = IdentityRestoreStageUi.PASSWORD)
                    report(Res.string.feature_identity_restore_failed)
                }
            } finally {
                password.fill('\u0000')
            }
        }
    }

    fun finish() {
        val stage = _uiState.value.stage
        if (stage != IdentityRestoreStageUi.COMPLETED && stage != IdentityRestoreStageUi.DISMISSED) return
        reset()
        navigator.popBackStack()
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
