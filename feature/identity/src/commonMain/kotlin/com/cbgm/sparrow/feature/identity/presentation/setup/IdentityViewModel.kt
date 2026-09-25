package com.cbgm.sparrow.feature.identity.presentation.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackupStatus
import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.feature.identity.domain.usecase.CreateIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityBackupStatusUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityStatusUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetLocalPhoneNumberUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetPublicIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.MarkIdentityBackupExportedUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.NormalizeLocalPhoneNumberUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.PrepareIdentityBackupUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.RestoreIdentityBackupUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.SaveLocalPhoneNameUseCase
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityBackupUiState
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityBackupUiStatus
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityUiEvent
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IdentityViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getIdentityStatus: GetIdentityStatusUseCase,
    private val getPublicIdentity: GetPublicIdentityUseCase,
    private val createIdentity: CreateIdentityUseCase,
    private val getLocalPhoneNumber: GetLocalPhoneNumberUseCase,
    private val normalizeLocalPhoneNumber: NormalizeLocalPhoneNumberUseCase,
    private val saveLocalPhoneName: SaveLocalPhoneNameUseCase,
    private val prepareIdentityBackup: PrepareIdentityBackupUseCase,
    private val restoreIdentityBackup: RestoreIdentityBackupUseCase,
    private val markIdentityBackupExported: MarkIdentityBackupExportedUseCase,
    private val getIdentityBackupStatus: GetIdentityBackupStatusUseCase
) : BaseViewModel() {
    val logger = SparrowLog.withTag("IdentityViewModel")
    private val _uiState = MutableStateFlow<IdentityUiState>(IdentityUiState.Loading)
    val uiState: StateFlow<IdentityUiState> = _uiState.asStateFlow()

    private val _backupState = MutableStateFlow(IdentityBackupUiState())
    val backupState: StateFlow<IdentityBackupUiState> = _backupState.asStateFlow()

    private val _exportDocument = MutableStateFlow<ByteArray?>(null)
    val exportDocument: StateFlow<ByteArray?> = _exportDocument.asStateFlow()

    private var pendingSigningPublicKey: ByteArray? = null
    private var pendingEncryptionPublicKey: ByteArray? = null

    init {
        loadIdentityState()
    }

    fun onUiEvent(event: IdentityUiEvent) {
        when (event) {
            IdentityUiEvent.RequestPhoneNumberHint -> Unit
            is IdentityUiEvent.PhoneNumberChanged -> updatePhoneNumber(
                event.value,
                errorMessage = null
            )

            is IdentityUiEvent.NameChanged -> updateName(event.value)
            IdentityUiEvent.CreateIdentityClicked -> createNewIdentity()
            IdentityUiEvent.RetryClicked -> loadIdentityState()
            IdentityUiEvent.ShareIdentityClicked -> navigator.navigateTo(AppRoute.ShareIdentity)
            IdentityUiEvent.OpenBackup -> navigator.navigateTo(AppRoute.IdentityBackup)
            IdentityUiEvent.OpenKeys -> navigator.navigateTo(AppRoute.IdentityKeys)
            IdentityUiEvent.BackClicked -> navigator.popBackStack()
        }
    }

    /** Passwords are never retained in the ViewModel or SavedStateHandle. */
    fun prepareBackup(password: CharArray) {
        val ready = _uiState.value as? IdentityUiState.Ready
        if (ready == null || _backupState.value.busy || _exportDocument.value != null) {
            password.fill('\u0000')
            return
        }
        pendingSigningPublicKey = ready.publicIdentity.signingPublicKey.copyOf()
        pendingEncryptionPublicKey = ready.publicIdentity.encryptionPublicKey.copyOf()
        viewModelScope.launch {
            _backupState.value = _backupState.value.copy(busy = true, message = null)
            try {
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

    fun backupFileWritten(success: Boolean, failure: String?) {
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
                        setBackupResult(
                            "Identity backup exported. Keep the file and password safe.",
                            isError = false
                        )
                    }
                    .onFailure { error ->
                        setBackupResult(
                            error.message ?: "Backup status could not be saved",
                            isError = true
                        )
                    }
            } else {
                setBackupResult(failure ?: "Identity backup was not saved", isError = true)
            }
        }
    }

    fun showBackupError(message: String) {
        setBackupResult(message, isError = true)
    }

    fun restoreBackup(document: ByteArray, password: CharArray) {
        val state = _uiState.value as? IdentityUiState.NoIdentity ?: return
        if (_backupState.value.busy) {
            password.fill('\u0000')
            return
        }
        val normalized = normalizeLocalPhoneNumber(state.phoneNumber).getOrElse { error ->
            _uiState.value =
                state.copy(phoneNumberError = error.message ?: "Enter your phone number first")
            password.fill('\u0000')
            return
        }
        viewModelScope.launch {
            _backupState.value = _backupState.value.copy(busy = true, message = null)
            try {
                restoreIdentityBackup(document, password, normalized, state.name)
                    .onSuccess { publicIdentity ->
                        clearDraft()
                        _uiState.value = IdentityUiState.Ready(publicIdentity, normalized)
                        refreshBackupStatus()
                        setBackupResult(
                            "Original identity keys restored. Chats and contacts must be re-established.",
                            isError = false
                        )
                    }
                    .onFailure { error ->
                        setBackupResult(error.message ?: "Identity restore failed", isError = true)
                    }
            } catch (error: Throwable) {
                setBackupResult(error.message ?: "Identity restore failed", isError = true)
            } finally {
                password.fill('\u0000')
            }
        }
    }

    fun onSuggestedPhoneNumber(phoneNumber: String) {
        updatePhoneNumber(value = phoneNumber.trim(), errorMessage = null)
    }

    fun onPhoneNumberHintUnavailable() {
        val currentState = _uiState.value as? IdentityUiState.NoIdentity ?: return
        if (currentState.phoneNumber.isBlank()) {
            _uiState.value = currentState.copy(
                phoneNumberError = "No number was available from this device. Enter it manually."
            )
        }
    }

    fun onPhoneNumberHintFailed(message: String) {
        val currentState = _uiState.value as? IdentityUiState.NoIdentity ?: return
        _uiState.value = currentState.copy(
            phoneNumberError = message.ifBlank { "Phone number picker could not be opened" }
        )
    }

    private fun loadIdentityState() {
        viewModelScope.launch {
            _uiState.value = IdentityUiState.Loading
            getIdentityStatus()
                .onSuccess { status -> handleIdentityStatus(status) }
                .onFailure { error ->
                    _uiState.value = IdentityUiState.Error(
                        message = error.message ?: "Failed to load identity state"
                    )
                }
        }
    }

    private fun createNewIdentity() {
        val currentState = _uiState.value as? IdentityUiState.NoIdentity ?: return
        val normalizedPhoneNumber = normalizeLocalPhoneNumber(currentState.phoneNumber)
            .getOrElse { error ->
                _uiState.value = currentState.copy(
                    phoneNumberError = error.message ?: "Invalid phone number"
                )
                return
            }

        viewModelScope.launch {
            _uiState.value = IdentityUiState.Loading

            saveLocalPhoneName(normalizedPhoneNumber, currentState.name)
                .onFailure { error ->
                    _uiState.value = IdentityUiState.NoIdentity(
                        phoneNumber = normalizedPhoneNumber,
                        name = currentState.name,
                        phoneNumberError = error.message ?: "Phone number could not be saved"
                    )
                    return@launch
                }

            createIdentity()
                .onSuccess { publicIdentity ->
                    clearDraft()
                    refreshBackupStatus()
                    _uiState.value = IdentityUiState.Ready(
                        publicIdentity = publicIdentity,
                        localPhoneNumber = normalizedPhoneNumber
                    )
                }
                .onFailure { error ->
                    _uiState.value = IdentityUiState.Error(
                        message = error.message ?: "Failed to create identity"
                    )
                }
        }
    }

    private fun updatePhoneNumber(value: String, errorMessage: String? = null) {
        val currentState = _uiState.value as? IdentityUiState.NoIdentity ?: return
        savedStateHandle[PHONE_NUMBER_KEY] = value
        _uiState.value = currentState.copy(
            phoneNumber = value,
            phoneNumberError = errorMessage
        )
    }

    private fun updateName(value: String) {
        val currentState = _uiState.value as? IdentityUiState.NoIdentity ?: return
        savedStateHandle[NAME_KEY] = value
        _uiState.value = currentState.copy(name = value)
    }

    private suspend fun handleIdentityStatus(status: IdentityStatus) {
        when (status) {
            IdentityStatus.NOT_CREATED -> {
                val storedPhone = getLocalPhoneNumber().getOrNull().orEmpty()
                val phone = savedStateHandle.get<String>(PHONE_NUMBER_KEY) ?: storedPhone
                val name = savedStateHandle.get<String>(NAME_KEY).orEmpty()
                _uiState.value = IdentityUiState.NoIdentity(phoneNumber = phone, name = name)
            }

            IdentityStatus.INCOMPLETE -> {
                clearDraft()
                _uiState.value = IdentityUiState.IncompleteIdentity
            }

            IdentityStatus.READY -> {
                clearDraft()
                loadReadyIdentity()
            }
        }
    }

    private suspend fun loadReadyIdentity() {
        val localPhoneNumber = getLocalPhoneNumber().getOrNull()?.takeIf { it.isNotBlank() }
        if (localPhoneNumber == null) {
            _uiState.value = IdentityUiState.Error(
                message = "Identity exists, but the local phone number is missing. " +
                    "Clear app data once and complete onboarding again."
            )
            return
        }

        getPublicIdentity()
            .onSuccess { publicIdentity ->
                if (publicIdentity != null) {
                    _uiState.value = IdentityUiState.Ready(
                        publicIdentity = publicIdentity,
                        localPhoneNumber = localPhoneNumber
                    )
                    refreshBackupStatus()
                } else {
                    _uiState.value = IdentityUiState.IncompleteIdentity
                }
            }
            .onFailure { error ->
                _uiState.value = IdentityUiState.Error(
                    message = error.message ?: "Failed to load public identity"
                )
            }
    }

    private suspend fun refreshBackupStatus() {
        getIdentityBackupStatus().onSuccess { status ->
            _backupState.value = _backupState.value.copy(
                status = when (status) {
                    IdentityBackupStatus.NOT_BACKED_UP -> IdentityBackupUiStatus.NOT_BACKED_UP
                    IdentityBackupStatus.EXPORTED -> IdentityBackupUiStatus.EXPORTED
                    IdentityBackupStatus.IMPORTED -> IdentityBackupUiStatus.IMPORTED
                }
            )
        }
    }

    private fun setBackupResult(message: String?, isError: Boolean) {
        _backupState.value = _backupState.value.copy(
            busy = false,
            message = message,
            error = isError
        )
        logger.error { message ?: "" }
    }

    private fun clearDraft() {
        savedStateHandle.remove<String>(PHONE_NUMBER_KEY)
        savedStateHandle.remove<String>(NAME_KEY)
    }

    private companion object {
        const val PHONE_NUMBER_KEY = "phoneNumber"
        const val NAME_KEY = "name"
    }
}
