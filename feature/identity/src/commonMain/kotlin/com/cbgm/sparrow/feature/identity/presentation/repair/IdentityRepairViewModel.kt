package com.cbgm.sparrow.feature.identity.presentation.repair

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackupMismatchException
import com.cbgm.sparrow.feature.identity.domain.usecase.RecoverIncompleteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.restore.ReadIdentityRestoreDocumentUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.restore.RepairIdentityFromBackupUseCase
import com.cbgm.sparrow.feature.identity.presentation.repair.model.IdentityRepairStageUi
import com.cbgm.sparrow.feature.identity.presentation.repair.model.IdentityRepairUiEvent
import com.cbgm.sparrow.feature.identity.presentation.repair.model.IdentityRepairUiState
import com.cbgm.sparrow.feature.media.presentation.filepicker.FilePickerLauncher
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerSessionResultUi
import com.cbgm.sparrow.feature.media.presentation.model.FileMediaSelectionUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_identity_repair_backup_mismatch
import com.cbgm.sparrow.resources.feature_identity_repair_failed
import com.cbgm.sparrow.resources.feature_identity_repair_reset_failed
import com.cbgm.sparrow.resources.feature_identity_restore_read_failed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/** The keys of the identity are lost for good: restore them from the identity's backup, or reset. */
class IdentityRepairViewModel(
    private val filePicker: FilePickerLauncher,
    private val readDocument: ReadIdentityRestoreDocumentUseCase,
    private val repairIdentityFromBackup: RepairIdentityFromBackupUseCase,
    private val resetIdentity: RecoverIncompleteIdentityUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(IdentityRepairUiState())
    val uiState = _uiState.asStateFlow()
    private var document: ByteArray? = null

    init {
        viewModelScope.launch {
            filePicker.results.collect {
                val result = filePicker.consumeResult() ?: return@collect
                if (_uiState.value.stage != IdentityRepairStageUi.SELECTING) return@collect
                when (result) {
                    is FilePickerSessionResultUi.Completed -> readSelection(result)
                    is FilePickerSessionResultUi.Dismissed -> backToIdle()
                    is FilePickerSessionResultUi.Failed -> failed(Res.string.feature_identity_restore_read_failed)
                }
            }
        }
    }

    fun onUiEvent(event: IdentityRepairUiEvent) {
        when (event) {
            IdentityRepairUiEvent.RestoreClicked -> selectBackup()
            is IdentityRepairUiEvent.PasswordChanged -> changePassword(event.value)
            IdentityRepairUiEvent.PasswordConfirmed -> repair()
            IdentityRepairUiEvent.PasswordDismissed -> leave(IdentityRepairStageUi.PASSWORD)
            IdentityRepairUiEvent.ResetClicked -> askToReset()
            IdentityRepairUiEvent.ResetConfirmed -> reset()
            IdentityRepairUiEvent.ResetDismissed -> leave(IdentityRepairStageUi.CONFIRM_RESET)
        }
    }

    private fun changePassword(value: String) {
        if (_uiState.value.stage == IdentityRepairStageUi.PASSWORD) _uiState.update { it.copy(password = value) }
    }

    private fun askToReset() {
        if (_uiState.value.stage == IdentityRepairStageUi.IDLE) {
            _uiState.value = IdentityRepairUiState(IdentityRepairStageUi.CONFIRM_RESET)
        }
    }

    private fun leave(stage: IdentityRepairStageUi) {
        if (_uiState.value.stage == stage) backToIdle()
    }

    private fun selectBackup() {
        if (_uiState.value.stage != IdentityRepairStageUi.IDLE) return
        _uiState.value = IdentityRepairUiState(IdentityRepairStageUi.SELECTING)
        val sessionId =
            filePicker.launch(
                maxItems = 1,
                maxFileBytes = ReadIdentityRestoreDocumentUseCase.MAX_DOCUMENT_BYTES,
                blockedSourceReferences = emptySet()
            )
        navigator.navigateTo(AppRoute.FilePicker(sessionId))
    }

    private suspend fun readSelection(result: FilePickerSessionResultUi.Completed) {
        val file = result.media.singleOrNull() as? FileMediaSelectionUi
        if (file == null) {
            failed(Res.string.feature_identity_restore_read_failed)
            return
        }
        _uiState.value = IdentityRepairUiState(IdentityRepairStageUi.READING)
        readDocument(file.localFilePath, file.byteSize)
            .onSuccess {
                document = it
                _uiState.value = IdentityRepairUiState(IdentityRepairStageUi.PASSWORD)
            }.onFailure { failed(Res.string.feature_identity_restore_read_failed) }
    }

    private fun repair() {
        val state = _uiState.value
        val document = document ?: return
        if (!state.canConfirmPassword) return
        val password = state.password.toCharArray()
        _uiState.value = IdentityRepairUiState(IdentityRepairStageUi.REPAIRING)
        viewModelScope.launch {
            val result =
                try {
                    repairIdentityFromBackup(document, password)
                } finally {
                    password.fill('\u0000')
                }
            result
                .onSuccess {
                    clearDocument()
                    _uiState.value = IdentityRepairUiState(IdentityRepairStageUi.FINISHED)
                }.onFailure { error ->
                    _uiState.value = IdentityRepairUiState(IdentityRepairStageUi.PASSWORD)
                    report(repairFailureMessage(error))
                }
        }
    }

    private fun reset() {
        if (_uiState.value.stage != IdentityRepairStageUi.CONFIRM_RESET) return
        _uiState.value = IdentityRepairUiState(IdentityRepairStageUi.RESETTING)
        viewModelScope.launch {
            resetIdentity()
                .onSuccess { _uiState.value = IdentityRepairUiState(IdentityRepairStageUi.FINISHED) }
                .onFailure { failed(Res.string.feature_identity_repair_reset_failed) }
        }
    }

    private fun repairFailureMessage(error: Throwable): StringResource =
        when (error) {
            is IdentityBackupMismatchException -> Res.string.feature_identity_repair_backup_mismatch
            else -> Res.string.feature_identity_repair_failed
        }

    private suspend fun failed(message: StringResource) {
        backToIdle()
        report(message)
    }

    private fun backToIdle() {
        clearDocument()
        _uiState.value = IdentityRepairUiState()
    }

    private suspend fun report(message: StringResource) {
        SparrowLog.showError(getString(message))
    }

    private fun clearDocument() {
        document?.fill(0)
        document = null
    }

    override fun onCleared() {
        clearDocument()
    }
}
