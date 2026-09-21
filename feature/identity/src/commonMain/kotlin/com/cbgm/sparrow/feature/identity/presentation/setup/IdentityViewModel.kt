package com.cbgm.sparrow.feature.identity.presentation.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackupStatus
import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.feature.identity.domain.usecase.ConfirmPendingRemoteIdentityChangeFingerprintUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.CreateIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.DismissPendingRemoteIdentityChangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityBackupStatusUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityStatusUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetLocalPhoneNumberUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetPublicIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.MarkIdentityBackupExportedUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.NormalizeLocalPhoneNumberUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObservePendingRemoteIdentityChangesUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.PrepareIdentityBackupUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.RestoreIdentityBackupUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.SaveLocalPhoneNameUseCase
import com.cbgm.sparrow.feature.identity.presentation.setup.mapper.toReviewUi
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityBackupUiState
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityBackupUiStatus
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityUiEvent
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityUiState
import com.cbgm.sparrow.feature.identity.presentation.setup.model.PendingIdentityReviewUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    private val getIdentityBackupStatus: GetIdentityBackupStatusUseCase,
    private val observePendingRemoteIdentityChanges: ObservePendingRemoteIdentityChangesUseCase,
    private val getRemoteIdentityForReview: GetRemoteIdentityUseCase,
    private val dismissPendingRemoteIdentityChange: DismissPendingRemoteIdentityChangeUseCase,
    private val confirmPendingRemoteIdentityChangeFingerprint: ConfirmPendingRemoteIdentityChangeFingerprintUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<IdentityUiState>(IdentityUiState.Loading)

    val uiState: StateFlow<IdentityUiState> = _uiState.asStateFlow()
    private val _pendingIdentityReview = MutableStateFlow(PendingIdentityReviewUiState())
    val pendingIdentityReview: StateFlow<PendingIdentityReviewUiState> = _pendingIdentityReview.asStateFlow()

    fun confirmIdentityChangeFingerprint(peerId: String, invitationId: String, independentlyCheckedFingerprint: String) {
        if (_pendingIdentityReview.value.confirmingInvitationId != null ||
            _pendingIdentityReview.value.dismissingInvitationId != null ||
            _pendingIdentityReview.value.requests.none {
                it.peerId == peerId && it.invitationId == invitationId && !it.fingerprintConfirmed
            }
        ) {
            return
        }
        viewModelScope.launch {
            _pendingIdentityReview.value = _pendingIdentityReview.value.copy(
                confirmingInvitationId = invitationId,
                errorMessage = null
            )
            confirmPendingRemoteIdentityChangeFingerprint(peerId, invitationId, independentlyCheckedFingerprint)
                .onFailure { error ->
                    _pendingIdentityReview.value = _pendingIdentityReview.value.copy(
                        errorMessage = error.message ?: "Fingerprint confirmation failed"
                    )
                }
            _pendingIdentityReview.value = _pendingIdentityReview.value.copy(confirmingInvitationId = null)
        }
    }

    fun dismissIdentityChange(peerId: String, invitationId: String) {
        if (_pendingIdentityReview.value.dismissingInvitationId != null ||
            _pendingIdentityReview.value.confirmingInvitationId != null ||
            _pendingIdentityReview.value.requests.none { it.peerId == peerId && it.invitationId == invitationId }
        ) {
            return
        }
        viewModelScope.launch {
            _pendingIdentityReview.value = _pendingIdentityReview.value.copy(
                dismissingInvitationId = invitationId,
                errorMessage = null
            )
            dismissPendingRemoteIdentityChange(peerId, invitationId)
                .onFailure { error ->
                    _pendingIdentityReview.value = _pendingIdentityReview.value.copy(
                        errorMessage = error.message ?: "Could not dismiss identity change"
                    )
                }
            _pendingIdentityReview.value = _pendingIdentityReview.value.copy(dismissingInvitationId = null)
        }
    }

    private fun observeIdentityChangeReviews() {
        viewModelScope.launch {
            try {
                observePendingRemoteIdentityChanges().collectLatest { candidates ->
                    val requests = candidates.map { candidate ->
                        // Show the OLD key alongside the proposed key, never treat an invitation
                        // signature as proof that the newly proposed key belongs to the old person.
                        val previous = getRemoteIdentityForReview(candidate.peerId).getOrThrow()
                        candidate.toReviewUi(previous)
                    }
                    _pendingIdentityReview.value = _pendingIdentityReview.value.copy(
                        requests = requests,
                        errorMessage = null
                    )
                }
            } catch (error: Exception) {
                _pendingIdentityReview.value = _pendingIdentityReview.value.copy(
                    errorMessage = error.message ?: "Could not load identity change requests"
                )
            }
        }
    }

    private val _backupState = MutableStateFlow(IdentityBackupUiState())
    val backupState: StateFlow<IdentityBackupUiState> = _backupState.asStateFlow()
    private val _exportDocument = MutableStateFlow<ByteArray?>(null)
    private var pendingSigningPublicKey: ByteArray? = null
    private var pendingEncryptionPublicKey: ByteArray? = null
    val exportDocument: StateFlow<ByteArray?> = _exportDocument.asStateFlow()

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
                prepareIdentityBackup(password).onSuccess { _exportDocument.value = it }
                    .onFailure {
                        pendingSigningPublicKey = null
                        pendingEncryptionPublicKey = null
                        _backupState.value = _backupState.value.copy(busy = false, message = it.message ?: "Backup encryption failed", error = true)
                    }
            } finally {
                password.fill('\u0000')
            }
        }
    }

    fun backupFileWritten(success: Boolean, failure: String?) {
        // The document picker and output stream must both succeed before the backup status changes.
        val bytes = _exportDocument.value ?: return
        _exportDocument.value = null
        bytes.fill(0)
        val expectedSigning = pendingSigningPublicKey
        val expectedEncryption = pendingEncryptionPublicKey
        pendingSigningPublicKey = null
        pendingEncryptionPublicKey = null
        viewModelScope.launch {
            if (success && expectedSigning != null && expectedEncryption != null) {
                markIdentityBackupExported(expectedSigning, expectedEncryption).onSuccess {
                    refreshBackupStatus()
                    _backupState.value = _backupState.value.copy(busy = false, message = "Identity backup exported. Keep the file and password safe.", error = false)
                }.onFailure {
                    _backupState.value = _backupState.value.copy(busy = false, message = it.message ?: "Backup status could not be saved", error = true)
                }
            } else {
                _backupState.value = _backupState.value.copy(busy = false, message = failure ?: "Identity backup was not saved", error = true)
            }
        }
    }

    fun showBackupError(message: String) {
        _backupState.value = _backupState.value.copy(busy = false, message = message, error = true)
    }

    fun restoreBackup(document: ByteArray, password: CharArray) {
        val state = _uiState.value as? IdentityUiState.NoIdentity ?: return

        if (_backupState.value.busy) {
            password.fill('\u0000')
            return
        }
        val normalized = normalizeLocalPhoneNumber(state.phoneNumber).getOrElse { error ->
            _uiState.value = state.copy(phoneNumberError = error.message ?: "Enter your phone number first")
            password.fill('\u0000')
            return
        }
        viewModelScope.launch {
            _backupState.value = _backupState.value.copy(busy = true, message = null)
            // The restore use case decrypts first, then persists the profile and validated keys.
            // A wrong password leaves the saved phone/profile and identity keys unchanged.
            try {
                restoreIdentityBackup(document, password, normalized, state.name).onSuccess { publicIdentity ->
                    clearDraft()
                    _uiState.value = IdentityUiState.Ready(publicIdentity, normalized)
                    refreshBackupStatus()
                    _backupState.value = _backupState.value.copy(busy = false, message = "Original identity keys restored. Chats and contacts must be re-established.", error = false)
                }.onFailure {
                    _backupState.value = _backupState.value.copy(busy = false, message = it.message ?: "Identity restore failed", error = true)
                }
            } catch (error: Throwable) {
                _backupState.value = _backupState.value.copy(busy = false, message = error.message ?: "Identity restore failed", error = true)
            } finally {
                password.fill('\u0000')
            }
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

    init {
        loadIdentityState()
        observeIdentityChangeReviews()
    }

    fun onUiEvent(event: IdentityUiEvent) {
        when (event) {
            IdentityUiEvent.RequestPhoneNumberHint -> Unit
            is IdentityUiEvent.PhoneNumberChanged ->
                updatePhoneNumber(
                    value = event.value,
                    errorMessage = null
                )
            is IdentityUiEvent.NameChanged -> updateName(event.value)
            IdentityUiEvent.CreateIdentityClicked -> createNewIdentity()
            IdentityUiEvent.RetryClicked -> loadIdentityState()
            IdentityUiEvent.ShareIdentityClicked -> navigator.navigateTo(AppRoute.ShareIdentity)
        }
    }

    private fun loadIdentityState() {
        viewModelScope.launch {
            _uiState.value = IdentityUiState.Loading

            getIdentityStatus()
                .onSuccess { status ->
                    handleIdentityStatus(status = status)
                }.onFailure { error ->
                    _uiState.value =
                        IdentityUiState.Error(
                            message = error.message ?: "Failed to load identity state"
                        )
                }
        }
    }

    fun onSuggestedPhoneNumber(phoneNumber: String) {
        updatePhoneNumber(
            value = phoneNumber.trim(),
            errorMessage = null
        )
    }

    fun onPhoneNumberHintUnavailable() {
        val currentState = _uiState.value

        if (currentState is IdentityUiState.NoIdentity && currentState.phoneNumber.isBlank()) {
            _uiState.value =
                currentState.copy(phoneNumberError = "No number was available from this device. Enter it manually.")
        }
    }

    fun onPhoneNumberHintFailed(message: String) {
        val currentState = _uiState.value

        if (currentState is IdentityUiState.NoIdentity) {
            _uiState.value =
                currentState.copy(phoneNumberError = message.ifBlank { "Phone number picker could not be opened" })
        }
    }

    private fun createNewIdentity() {
        val currentState = _uiState.value

        if (currentState !is IdentityUiState.NoIdentity) return

        val normalizedPhoneNumber =
            normalizeLocalPhoneNumber(phoneNumber = currentState.phoneNumber)
                .getOrElse { error ->
                    _uiState.value =
                        currentState.copy(phoneNumberError = error.message ?: "Invalid phone number")

                    return
                }

        viewModelScope.launch {
            _uiState.value = IdentityUiState.Loading

            saveLocalPhoneName(phoneNumber = normalizedPhoneNumber, name = currentState.name)
                .onFailure { error ->
                    _uiState.value =
                        IdentityUiState.NoIdentity(
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
                    _uiState.value =
                        IdentityUiState.Ready(
                            publicIdentity = publicIdentity,
                            localPhoneNumber = normalizedPhoneNumber
                        )
                }.onFailure { error ->
                    _uiState.value =
                        IdentityUiState.Error(
                            message = error.message ?: "Failed to create identity"
                        )
                }
        }
    }

    private fun updatePhoneNumber(
        value: String,
        errorMessage: String?
    ) {
        val currentState = _uiState.value

        if (currentState is IdentityUiState.NoIdentity) {
            savedStateHandle[PHONE_NUMBER_KEY] = value
            _uiState.value =
                currentState.copy(
                    phoneNumber = value,
                    phoneNumberError = errorMessage
                )
        }
    }

    private fun updateName(value: String) {
        val currentState = _uiState.value

        if (currentState is IdentityUiState.NoIdentity) {
            savedStateHandle[NAME_KEY] = value
            _uiState.value =
                currentState.copy(
                    name = value
                )
        }
    }

    private suspend fun handleIdentityStatus(status: IdentityStatus) {
        when (status) {
            IdentityStatus.NOT_CREATED -> {
                val storedPhoneNumber = getLocalPhoneNumber().getOrNull().orEmpty()
                val phoneNumber =
                    if (savedStateHandle.contains(PHONE_NUMBER_KEY)) {
                        savedStateHandle.get<String>(PHONE_NUMBER_KEY).orEmpty()
                    } else {
                        storedPhoneNumber
                    }
                _uiState.value =
                    IdentityUiState.NoIdentity(
                        phoneNumber = phoneNumber,
                        name = savedStateHandle.get<String>(NAME_KEY).orEmpty()
                    )
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

    private fun clearDraft() {
        savedStateHandle.remove<String>(PHONE_NUMBER_KEY)
        savedStateHandle.remove<String>(NAME_KEY)
    }

    private suspend fun loadReadyIdentity() {
        val localPhoneNumber =
            getLocalPhoneNumber()
                .getOrElse { error ->
                    _uiState.value =
                        IdentityUiState.Error(
                            message = error.message ?: "Local phone number could not be loaded"
                        )

                    return
                }?.takeIf {
                    it.isNotBlank()
                }

        if (localPhoneNumber == null) {
            _uiState.value =
                IdentityUiState.Error(
                    message =
                        "Identity exists, but the local phone number is missing. " +
                            "Clear app data once and complete onboarding again."
                )

            return
        }

        getPublicIdentity()
            .onSuccess { publicIdentity ->
                _uiState.value =
                    if (publicIdentity != null) {
                        IdentityUiState.Ready(
                            publicIdentity = publicIdentity,
                            localPhoneNumber = localPhoneNumber
                        )
                    } else {
                        IdentityUiState.IncompleteIdentity
                    }
            }.onFailure { error ->
                _uiState.value =
                    IdentityUiState.Error(
                        message = error.message ?: "Failed to load public identity"
                    )
            }
        if (_uiState.value is IdentityUiState.Ready) refreshBackupStatus()
    }

    private companion object {
        const val PHONE_NUMBER_KEY = "phoneNumber"
        const val NAME_KEY = "name"
    }
}
