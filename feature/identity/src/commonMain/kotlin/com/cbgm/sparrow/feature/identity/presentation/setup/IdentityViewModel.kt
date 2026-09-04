package com.cbgm.sparrow.feature.identity.presentation.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.feature.identity.domain.usecase.CreateIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityStatusUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetLocalPhoneNumberUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetPublicIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.NormalizeLocalPhoneNumberUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.SaveLocalPhoneNameUseCase
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
    private val saveLocalPhoneName: SaveLocalPhoneNameUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<IdentityUiState>(IdentityUiState.Loading)

    val uiState: StateFlow<IdentityUiState> = _uiState.asStateFlow()

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
    }

    private companion object {
        const val PHONE_NUMBER_KEY = "phoneNumber"
        const val NAME_KEY = "name"
    }
}
