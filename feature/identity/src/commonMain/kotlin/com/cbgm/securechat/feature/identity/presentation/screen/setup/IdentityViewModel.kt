package com.cbgm.securechat.feature.identity.presentation.screen.setup

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.identity.domain.model.IdentityStatus
import com.cbgm.securechat.feature.identity.domain.usecase.CreateIdentity
import com.cbgm.securechat.feature.identity.domain.usecase.GetIdentityStatus
import com.cbgm.securechat.feature.identity.domain.usecase.GetLocalPhoneNumber
import com.cbgm.securechat.feature.identity.domain.usecase.GetPublicIdentity
import com.cbgm.securechat.feature.identity.domain.usecase.NormalizeLocalPhoneNumber
import com.cbgm.securechat.feature.identity.domain.usecase.SaveLocalPhoneName
import com.cbgm.securechat.feature.identity.presentation.model.IdentityUiEvent
import com.cbgm.securechat.feature.identity.presentation.model.IdentityUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IdentityViewModel(
    private val getIdentityStatus: GetIdentityStatus,
    private val getPublicIdentity: GetPublicIdentity,
    private val createIdentity: CreateIdentity,
    private val getLocalPhoneNumber: GetLocalPhoneNumber,
    private val normalizeLocalPhoneNumber: NormalizeLocalPhoneNumber,
    private val saveLocalPhoneName: SaveLocalPhoneName
) : BaseViewModel() {
    private val mutableUiState = MutableStateFlow<IdentityUiState>(IdentityUiState.Loading)

    val uiState: StateFlow<IdentityUiState> = mutableUiState.asStateFlow()

    init {
        loadIdentityState()
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
            mutableUiState.value = IdentityUiState.Loading

            getIdentityStatus()
                .onSuccess { status ->
                    handleIdentityStatus(status = status)
                }.onFailure { error ->
                    mutableUiState.value =
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
        val currentState = mutableUiState.value

        if (currentState is IdentityUiState.NoIdentity && currentState.phoneNumber.isBlank()) {
            mutableUiState.value =
                currentState.copy(phoneNumberError = "No number was available from this device. Enter it manually.")
        }
    }

    fun onPhoneNumberHintFailed(message: String) {
        val currentState = mutableUiState.value

        if (currentState is IdentityUiState.NoIdentity) {
            mutableUiState.value =
                currentState.copy(phoneNumberError = message.ifBlank { "Phone number picker could not be opened" })
        }
    }

    private fun createNewIdentity() {
        val currentState = mutableUiState.value

        if (currentState !is IdentityUiState.NoIdentity) return

        val normalizedPhoneNumber =
            normalizeLocalPhoneNumber(phoneNumber = currentState.phoneNumber)
                .getOrElse { error ->
                    mutableUiState.value =
                        currentState.copy(phoneNumberError = error.message ?: "Invalid phone number")

                    return
                }

        viewModelScope.launch {
            mutableUiState.value = IdentityUiState.Loading

            saveLocalPhoneName(phoneNumber = normalizedPhoneNumber, name = currentState.name)
                .onFailure { error ->
                    mutableUiState.value =
                        IdentityUiState.NoIdentity(
                            phoneNumber = normalizedPhoneNumber,
                            name = currentState.name,
                            phoneNumberError = error.message ?: "Phone number could not be saved"
                        )

                    return@launch
                }

            createIdentity()
                .onSuccess { publicIdentity ->
                    mutableUiState.value =
                        IdentityUiState.Ready(
                            publicIdentity = publicIdentity,
                            localPhoneNumber = normalizedPhoneNumber
                        )
                }.onFailure { error ->
                    mutableUiState.value =
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
        val currentState = mutableUiState.value

        if (currentState is IdentityUiState.NoIdentity) {
            mutableUiState.value =
                currentState.copy(
                    phoneNumber = value,
                    phoneNumberError = errorMessage
                )
        }
    }

    private fun updateName(value: String) {
        val currentState = mutableUiState.value

        if (currentState is IdentityUiState.NoIdentity) {
            mutableUiState.value =
                currentState.copy(
                    name = value
                )
        }
    }

    private suspend fun handleIdentityStatus(status: IdentityStatus) {
        when (status) {
            IdentityStatus.NOT_CREATED -> {
                val storedPhoneNumber =
                    getLocalPhoneNumber().getOrNull().orEmpty()

                mutableUiState.value = IdentityUiState.NoIdentity(phoneNumber = storedPhoneNumber)
            }

            IdentityStatus.INCOMPLETE -> {
                mutableUiState.value = IdentityUiState.IncompleteIdentity
            }

            IdentityStatus.READY -> {
                loadReadyIdentity()
            }
        }
    }

    private suspend fun loadReadyIdentity() {
        val localPhoneNumber =
            getLocalPhoneNumber()
                .getOrElse { error ->
                    mutableUiState.value =
                        IdentityUiState.Error(
                            message = error.message ?: "Local phone number could not be loaded"
                        )

                    return
                }?.takeIf {
                    it.isNotBlank()
                }

        if (localPhoneNumber == null) {
            mutableUiState.value =
                IdentityUiState.Error(
                    message =
                        "Identity exists, but the local phone number is missing. " +
                            "Clear app data once and complete onboarding again."
                )

            return
        }

        getPublicIdentity()
            .onSuccess { publicIdentity ->
                mutableUiState.value =
                    if (publicIdentity != null) {
                        IdentityUiState.Ready(
                            publicIdentity = publicIdentity,
                            localPhoneNumber = localPhoneNumber
                        )
                    } else {
                        IdentityUiState.IncompleteIdentity
                    }
            }.onFailure { error ->
                mutableUiState.value =
                    IdentityUiState.Error(
                        message = error.message ?: "Failed to load public identity"
                    )
            }
    }
}
