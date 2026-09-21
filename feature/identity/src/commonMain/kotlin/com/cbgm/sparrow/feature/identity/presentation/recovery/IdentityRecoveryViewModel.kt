package com.cbgm.sparrow.feature.identity.presentation.recovery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.extensions.toFingerprint
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.identity.domain.usecase.ApprovePendingRemoteIdentityChangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ConfirmPendingRemoteIdentityChangeFingerprintUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.DecodeSharedIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.DismissPendingRemoteIdentityChangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObservePendingRemoteIdentityChangesUseCase
import com.cbgm.sparrow.feature.identity.presentation.setup.mapper.toReviewUi
import com.cbgm.sparrow.feature.identity.presentation.setup.model.PendingIdentityReviewUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A dedicated recovery screen; no remote identity data/model is sent to the user's own Identity screen. */
data class IdentityRecoveryUiState(
    val request: PendingIdentityReviewUi? = null,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val approved: Boolean = false,
    val invitationQueued: Boolean = false,
    val error: String? = null
)

class IdentityRecoveryViewModel(
    savedStateHandle: SavedStateHandle,
    observePending: ObservePendingRemoteIdentityChangesUseCase,
    private val getRemoteIdentity: GetRemoteIdentityUseCase,
    private val decodeSharedIdentity: DecodeSharedIdentityUseCase,
    private val confirmFingerprint: ConfirmPendingRemoteIdentityChangeFingerprintUseCase,
    private val approveReplacement: ApprovePendingRemoteIdentityChangeUseCase,
    private val dismissRequest: DismissPendingRemoteIdentityChangeUseCase
) : BaseViewModel() {
    private val peerId = savedStateHandle.requireRouteArgument<String>(AppRoute.IdentityRecovery::peerId.name)
    private val invitationId = savedStateHandle.requireRouteArgument<String>(AppRoute.IdentityRecovery::invitationId.name)
    private val _uiState = MutableStateFlow(IdentityRecoveryUiState())
    val uiState: StateFlow<IdentityRecoveryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observePending().collect { candidates ->
                if (_uiState.value.approved) return@collect
                val candidate = candidates.firstOrNull { it.peerId == peerId && it.invitationId == invitationId }
                if (candidate == null) {
                    _uiState.value = _uiState.value.copy(loading = false, request = null)
                } else {
                    getRemoteIdentity(peerId).fold(
                        onSuccess = { previous ->
                            _uiState.value = _uiState.value.copy(
                                request = candidate.toReviewUi(previous),
                                loading = false
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                loading = false,
                                error = error.message ?: "Could not load the previous identity"
                            )
                        }
                    )
                }
            }
        }
    }

    fun confirm(independentlyCheckedFingerprint: String) {
        val candidate = _uiState.value.request ?: return
        if (_uiState.value.busy || candidate.fingerprintConfirmed) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, error = null)
            confirmFingerprint(peerId, invitationId, independentlyCheckedFingerprint).fold(
                onSuccess = {
                    // Keep the SAME screen open and show the approval action without reopening a dialog.
                    _uiState.value = _uiState.value.copy(
                        request = candidate.copy(fingerprintConfirmed = true),
                        busy = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        busy = false,
                        error = error.message ?: "Fingerprint verification failed"
                    )
                }
            )
        }
    }

    /** Scan the actual QR displayed on the OTHER device, not a copy from this invitation. */
    fun confirmScannedIdentity(encodedIdentity: String) {
        val candidate = _uiState.value.request ?: return
        if (_uiState.value.busy || candidate.fingerprintConfirmed) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, error = null)
            decodeSharedIdentity(encodedIdentity).fold(
                onSuccess = { sharedIdentity ->
                    val scannedSigning = sharedIdentity.signingPublicKey.toFingerprint()
                    val scannedEncryption = sharedIdentity.encryptionPublicKey.toFingerprint()
                    if (scannedSigning != candidate.proposedSigningKey ||
                        scannedEncryption != candidate.proposedEncryptionKey
                    ) {
                        _uiState.value = _uiState.value.copy(
                            busy = false,
                            error = "Scanned identity does not match the proposed keys"
                        )
                        return@launch
                    }
                    confirmFingerprint(peerId, invitationId, scannedSigning).fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(
                                request = candidate.copy(fingerprintConfirmed = true),
                                busy = false
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                busy = false,
                                error = error.message ?: "QR verification failed"
                            )
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        busy = false,
                        error = error.message ?: "Invalid identity QR code"
                    )
                }
            )
        }
    }

    fun approve(startFreshInvitation: suspend (String) -> Result<Unit>) {
        val candidate = _uiState.value.request ?: return
        if (_uiState.value.busy || !candidate.fingerprintConfirmed || _uiState.value.approved) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, error = null)
            approveReplacement(peerId, invitationId).fold(
                onSuccess = {
                    // The identity cutover is complete; the old keys' authorization is not inherited.
                    _uiState.value = _uiState.value.copy(approved = true)
                    startFreshInvitation(peerId).fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(busy = false, invitationQueued = true)
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                busy = false,
                                error = error.message ?: "Could not start a fresh invitation"
                            )
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        busy = false,
                        error = error.message ?: "Identity replacement failed"
                    )
                }
            )
        }
    }

    /** Retry only the fresh invitation: approval must not be repeated after a successful key cutover. */
    fun retryReconnection(startFreshInvitation: suspend (String) -> Result<Unit>) {
        if (_uiState.value.busy || !_uiState.value.approved || _uiState.value.invitationQueued) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, error = null)
            startFreshInvitation(peerId).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(busy = false, invitationQueued = true) },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        busy = false,
                        error = error.message ?: "Could not reconnect"
                    )
                }
            )
        }
    }

    fun dismiss() {
        if (_uiState.value.busy || _uiState.value.approved) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, error = null)
            dismissRequest(peerId, invitationId).fold(
                onSuccess = { navigator.popBackStack() },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        busy = false,
                        error = error.message ?: "Could not dismiss recovery request"
                    )
                }
            )
        }
    }

    fun close() = navigator.popBackStack()
}
