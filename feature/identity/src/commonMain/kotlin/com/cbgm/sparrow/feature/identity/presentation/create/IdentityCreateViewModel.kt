package com.cbgm.sparrow.feature.identity.presentation.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.identity.domain.usecase.CreateIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityStatusUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetLocalPhoneNumberUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.NormalizeLocalPhoneNumberUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.SaveLocalPhoneNameUseCase
import com.cbgm.sparrow.feature.identity.presentation.create.mapper.toUi
import com.cbgm.sparrow.feature.identity.presentation.create.model.IdentityCreateUiEvent
import com.cbgm.sparrow.feature.identity.presentation.create.model.IdentityCreateUiState
import com.cbgm.sparrow.feature.identity.presentation.verification.model.PhoneVerificationResultUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_identity_create_failed
import com.cbgm.sparrow.resources.feature_identity_phone_hint_failed
import com.cbgm.sparrow.resources.feature_identity_phone_number_invalid
import com.cbgm.sparrow.resources.feature_identity_setup_load_failed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

class IdentityCreateViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getIdentityStatus: GetIdentityStatusUseCase,
    private val getLocalPhoneNumber: GetLocalPhoneNumberUseCase,
    private val normalizePhoneNumber: NormalizeLocalPhoneNumberUseCase,
    private val saveLocalPhoneName: SaveLocalPhoneNameUseCase,
    private val createIdentity: CreateIdentityUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<IdentityCreateUiState>(IdentityCreateUiState.Loading)
    val uiState = _uiState.asStateFlow()

    // The number waiting for SMS verification; the identity is created once it is verified.
    private val _phoneToVerify = MutableStateFlow<String?>(null)
    val phoneToVerify = _phoneToVerify.asStateFlow()
    private var verifying: Pair<IdentityCreateUiState.Form, String>? = null
    private var loadingIdentity = false

    init {
        refreshIdentity()
    }

    /** Re-read persisted state when returning from the restore destination. */
    fun refreshIdentity() {
        if (loadingIdentity || _uiState.value == IdentityCreateUiState.Creating ||
            _uiState.value == IdentityCreateUiState.Completed
        ) {
            return
        }
        loadingIdentity = true
        viewModelScope.launch {
            try {
                val form = _uiState.value as? IdentityCreateUiState.Form
                val phone = form?.phoneNumber ?: savedStateHandle.get<String>(PHONE_KEY)
                    ?: getLocalPhoneNumber().getOrElse {
                        showLoadFailure()
                        return@launch
                    }.orEmpty()
                val name = form?.name ?: savedStateHandle.get<String>(NAME_KEY).orEmpty()
                getIdentityStatus().onSuccess { status ->
                    val next = status.toUi(phone, name)
                    if (next == IdentityCreateUiState.Completed && phone.isBlank()) {
                        showLoadFailure()
                        return@onSuccess
                    }
                    if (_uiState.value != IdentityCreateUiState.Creating) {
                        _uiState.value = if (next is IdentityCreateUiState.Form) {
                            _uiState.value as? IdentityCreateUiState.Form ?: next
                        } else {
                            next
                        }
                        if (next == IdentityCreateUiState.Completed) clearDraft()
                    }
                }.onFailure { showLoadFailure() }
            } finally {
                loadingIdentity = false
            }
        }
    }

    fun onUiEvent(event: IdentityCreateUiEvent) {
        when (event) {
            is IdentityCreateUiEvent.PhoneNumberChanged -> updatePhoneNumber(event.value)
            is IdentityCreateUiEvent.NameChanged -> {
                val form = _uiState.value as? IdentityCreateUiState.Form ?: return
                savedStateHandle[NAME_KEY] = event.value
                _uiState.value = form.copy(name = event.value)
            }
            IdentityCreateUiEvent.CreateClicked -> verifyAndCreate()
        }
    }

    fun onSuggestedPhoneNumber(phoneNumber: String) = updatePhoneNumber(phoneNumber.trim())

    fun onPhoneNumberHintFailed() {
        viewModelScope.launch { showFailure(Res.string.feature_identity_phone_hint_failed) }
    }

    private suspend fun showLoadFailure() {
        _uiState.value = IdentityCreateUiState.Error
        showFailure(Res.string.feature_identity_setup_load_failed)
    }

    // The identity is created only after the phone number was verified.
    private fun verifyAndCreate() {
        val form = _uiState.value as? IdentityCreateUiState.Form ?: return
        val phone = normalizePhoneNumber(form.phoneNumber).getOrElse {
            viewModelScope.launch { showFailure(Res.string.feature_identity_phone_number_invalid) }
            return
        }
        verifying = form to phone
        _phoneToVerify.value = phone
    }

    fun onPhoneVerification(result: PhoneVerificationResultUi) {
        val (form, phone) = verifying ?: return
        verifying = null
        _phoneToVerify.value = null
        if (result == PhoneVerificationResultUi.Verified) {
            viewModelScope.launch { create(form, phone) }
        }
    }

    private suspend fun create(
        form: IdentityCreateUiState.Form,
        phone: String
    ) {
        _uiState.value = IdentityCreateUiState.Creating
        saveLocalPhoneName(phone, form.name).onFailure {
            _uiState.value = form
            showFailure(Res.string.feature_identity_create_failed)
            return
        }
        createIdentity().onSuccess {
            clearDraft()
            _uiState.value = IdentityCreateUiState.Completed
        }.onFailure {
            _uiState.value = form
            showFailure(Res.string.feature_identity_create_failed)
        }
    }

    private fun updatePhoneNumber(value: String) {
        val form = _uiState.value as? IdentityCreateUiState.Form ?: return
        savedStateHandle[PHONE_KEY] = value
        _uiState.value = form.copy(phoneNumber = value)
    }

    private fun clearDraft() {
        savedStateHandle.remove<String>(PHONE_KEY)
        savedStateHandle.remove<String>(NAME_KEY)
    }

    private suspend fun showFailure(resource: StringResource) {
        val message = getString(resource)
        SparrowLog.showError(message)
    }

    private companion object {
        const val PHONE_KEY = "phoneNumber"
        const val NAME_KEY = "name"
    }
}
