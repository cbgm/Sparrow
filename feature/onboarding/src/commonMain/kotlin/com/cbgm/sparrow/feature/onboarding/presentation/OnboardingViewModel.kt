package com.cbgm.sparrow.feature.onboarding.presentation

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.media.domain.repository.MediaSelectionFileRepository
import com.cbgm.sparrow.feature.media.presentation.filepicker.FilePickerLauncher
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerSessionResultUi
import com.cbgm.sparrow.feature.media.presentation.model.FileMediaSelectionUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionUi
import com.cbgm.sparrow.feature.onboarding.device.PermissionRequestResult
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingPage
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiEvent
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val filePicker: FilePickerLauncher,
    private val selectedFiles: MediaSelectionFileRepository
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    private var pendingBackupDocument: ByteArray? = null
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        observeFilePickerResults()
    }

    fun onUiEvent(event: OnboardingUiEvent) {
        when (event) {
            OnboardingUiEvent.NextClicked -> next()
            OnboardingUiEvent.RequestPermissionsClicked -> requestPermissions()
            OnboardingUiEvent.RetryAutomaticNumberClicked -> retryAutomaticPhoneNumber()
            OnboardingUiEvent.ChooseAnotherNumberClicked -> requestPhoneNumberHint()
            is OnboardingUiEvent.PhoneNumberChanged,
            is OnboardingUiEvent.NameChanged,
            OnboardingUiEvent.ApproveAndCreateClicked -> Unit
        }
    }

    fun openBackupRestorePicker() {
        val sessionId =
            filePicker.launch(
                maxItems = 1,
                maxFileBytes = MAX_IDENTITY_BACKUP_BYTES,
                blockedSourceReferences = emptySet()
            )
        navigator.navigateTo(AppRoute.FilePicker(sessionId))
    }

    fun onBackupPasswordChanged(value: String) {
        _uiState.update { state -> state.copy(backupPassword = value) }
    }

    fun dismissBackupRestore() {
        clearBackupRestoreState()
    }

    fun confirmBackupRestore(onRestore: (ByteArray, CharArray) -> Unit) {
        val document = pendingBackupDocument ?: return
        val password = _uiState.value.backupPassword.toCharArray()
        pendingBackupDocument = null
        clearBackupRestoreUiState()
        onRestore(document, password)
    }

    private fun observeFilePickerResults() {
        viewModelScope.launch {
            filePicker.results.collect {
                val result = filePicker.consumeResult() ?: return@collect
                handleFilePickerResult(result)
            }
        }
    }

    private suspend fun handleFilePickerResult(result: FilePickerSessionResultUi) {
        when (result) {
            is FilePickerSessionResultUi.Completed -> importBackup(result.media.singleOrNull())
            is FilePickerSessionResultUi.Failed -> setBackupImportError(result.message)
            is FilePickerSessionResultUi.Dismissed -> Unit
        }
    }

    private suspend fun importBackup(selection: MediaSelectionUi?) {
        val file = selection as? FileMediaSelectionUi
        if (file == null) {
            setBackupImportError("Identity backup must be a file")
            return
        }

        try {
            require(file.byteSize in 1..MAX_IDENTITY_BACKUP_BYTES) {
                "Identity backup is too large or empty"
            }
            val document = selectedFiles.read(file.localFilePath)
            require(document.size.toLong() in 1..MAX_IDENTITY_BACKUP_BYTES) {
                "Identity backup is too large or empty"
            }
            pendingBackupDocument = document
            _uiState.update { state ->
                state.copy(
                    isBackupRestoreVisible = true,
                    backupPassword = "",
                    backupImportError = null
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            setBackupImportError(error.message ?: "Could not read identity backup")
        } finally {
            try {
                selectedFiles.delete(file.localFilePath)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                setBackupImportError(error.message ?: "Could not remove temporary backup")
            }
        }
    }

    private fun setBackupImportError(message: String) {
        _uiState.update { state -> state.copy(backupImportError = message) }
    }

    private fun clearBackupRestoreState() {
        pendingBackupDocument?.fill(0)
        pendingBackupDocument = null
        clearBackupRestoreUiState()
    }

    private fun clearBackupRestoreUiState() {
        _uiState.update { state ->
            state.copy(
                isBackupRestoreVisible = false,
                backupPassword = "",
                backupImportError = null
            )
        }
    }

    private fun next() {
        _uiState.value =
            when (_uiState.value.page) {
                OnboardingPage.WELCOME -> _uiState.value.copy(page = OnboardingPage.PRIVACY)
                OnboardingPage.PRIVACY -> _uiState.value.copy(page = OnboardingPage.PERMISSIONS)
                OnboardingPage.PERMISSIONS -> _uiState.value.copy(page = OnboardingPage.PHONE)
                OnboardingPage.PHONE -> _uiState.value
            }
    }

    private fun requestPermissions() {
        _uiState.value =
            _uiState.value.copy(
                permissionRequestId = _uiState.value.permissionRequestId + 1
            )
    }

    fun onPermissionsResult(result: PermissionRequestResult) {
        _uiState.value =
            _uiState.value.copy(
                permissionsRequested = true,
                phonePermissionGranted = result.phoneNumberGranted,
                page = OnboardingPage.PHONE,
                automaticPhoneRequestId =
                    if (result.phoneNumberGranted) {
                        _uiState.value.automaticPhoneRequestId + 1
                    } else {
                        _uiState.value.automaticPhoneRequestId
                    }
            )
    }

    private fun requestPhoneNumberHint() {
        _uiState.update { state ->
            state.copy(phoneNumberHintRequestId = state.phoneNumberHintRequestId + 1)
        }
    }

    private fun retryAutomaticPhoneNumber() {
        if (!_uiState.value.phonePermissionGranted) return
        _uiState.value =
            _uiState.value.copy(
                automaticPhoneRequestId = _uiState.value.automaticPhoneRequestId + 1
            )
    }

    fun setCreatingIdentity(value: Boolean) {
        _uiState.value = _uiState.value.copy(isCreatingIdentity = value)
    }

    private companion object {
        const val MAX_IDENTITY_BACKUP_BYTES = 16_384L
    }
}
