package com.cbgm.sparrow.feature.identity.presentation.create

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
import com.cbgm.sparrow.feature.identity.presentation.backup.model.IdentityBackupUiState
import com.cbgm.sparrow.feature.identity.presentation.model.IdentityUiEvent
import com.cbgm.sparrow.feature.identity.presentation.model.IdentityUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IdentityCreateViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getIdentityStatus: GetIdentityStatusUseCase,
    private val getPublicIdentity: GetPublicIdentityUseCase,
    private val createIdentity: CreateIdentityUseCase,
    private val getLocalPhoneNumber: GetLocalPhoneNumberUseCase,
    private val normalizeLocalPhoneNumber: NormalizeLocalPhoneNumberUseCase,
    private val saveLocalPhoneName: SaveLocalPhoneNameUseCase
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

    private fun clearDraft() {
        savedStateHandle.remove<String>(PHONE_NUMBER_KEY)
        savedStateHandle.remove<String>(NAME_KEY)
    }

    private companion object {
        const val PHONE_NUMBER_KEY = "phoneNumber"
        const val NAME_KEY = "name"
    }
}
