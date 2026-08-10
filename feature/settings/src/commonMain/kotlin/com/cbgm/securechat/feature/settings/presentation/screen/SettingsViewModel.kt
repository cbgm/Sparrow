package com.cbgm.securechat.feature.settings.presentation.screen

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.security.DirectIdentitySetupMode
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.settings.domain.usecase.GetAppLanguageUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.GetBuildInfoUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.GetDeveloperEnabledUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.ObserveBlockUnknownContactInvites
import com.cbgm.securechat.feature.settings.domain.usecase.ObserveBlockedContactIds
import com.cbgm.securechat.feature.settings.domain.usecase.ObserveDirectIdentitySetupMode
import com.cbgm.securechat.feature.settings.domain.usecase.SetAppLanguageUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.SetBlockUnknownContactInvites
import com.cbgm.securechat.feature.settings.domain.usecase.SetDeveloperEnabledUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.SetDirectIdentitySetupMode
import com.cbgm.securechat.feature.settings.presentation.model.DEVELOPER_MODE_TAP_THRESHOLD
import com.cbgm.securechat.feature.settings.presentation.model.SettingsEffect
import com.cbgm.securechat.feature.settings.presentation.model.SettingsUiEvent
import com.cbgm.securechat.feature.settings.presentation.model.SettingsUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val setAppLanguageUseCase: SetAppLanguageUseCase,
    private val getAppLanguageUseCase: GetAppLanguageUseCase,
    private val getDeveloperEnabledUseCase: GetDeveloperEnabledUseCase,
    private val getBuildInfoUseCase: GetBuildInfoUseCase,
    private val setDeveloperModeEnabledUseCase: SetDeveloperEnabledUseCase,
    private val observeDirectIdentitySetupMode: ObserveDirectIdentitySetupMode,
    private val setDirectIdentitySetupMode: SetDirectIdentitySetupMode,
    private val observeBlockUnknownContactInvites: ObserveBlockUnknownContactInvites,
    private val setBlockUnknownContactInvites: SetBlockUnknownContactInvites,
    private val observeBlockedContactIds: ObserveBlockedContactIds
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadSettings()
        observeIdentitySetupMode()
        observeContactBlockingSettings()
    }

    fun onUiEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.LanguagePickerOpened ->
                _uiState.update { it.copy(showLanguagePicker = true) }
            SettingsUiEvent.LanguagePickerDismissed ->
                _uiState.update { it.copy(showLanguagePicker = false) }
            is SettingsUiEvent.LanguageSelected -> selectLanguage(event.language)
            is SettingsUiEvent.DirectIdentitySetupModeChanged -> changeDirectIdentitySetupMode(event.mode)
            is SettingsUiEvent.BlockUnknownContactInvitesChanged -> changeBlockUnknownContactInvites(event.enabled)
            SettingsUiEvent.PrivacyPolicyClicked -> navigator.navigateTo(AppRoute.PrivacyPolicy)
            SettingsUiEvent.DataDisclaimerClicked -> navigator.navigateTo(AppRoute.DataDisclaimer)
            SettingsUiEvent.LicensesClicked -> navigator.navigateTo(AppRoute.Licenses)
            SettingsUiEvent.DeveloperMenuClicked -> navigator.navigateTo(AppRoute.DeveloperMenu)
            SettingsUiEvent.BlockedContactsClicked -> navigator.navigateTo(AppRoute.BlockedContacts)
            SettingsUiEvent.ControlPlanesClicked -> navigator.navigateTo(AppRoute.ControlPlanes)
            SettingsUiEvent.VersionRowTapped -> handleVersionTap()
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentLanguage = getAppLanguageUseCase(),
                    isDeveloperModeEnabled = getDeveloperEnabledUseCase(),
                    buildInfo = getBuildInfoUseCase()
                )
            }
        }
    }

    private fun observeIdentitySetupMode() {
        viewModelScope.launch {
            observeDirectIdentitySetupMode().collect { mode ->
                _uiState.update {
                    it.copy(directIdentitySetupMode = mode)
                }
            }
        }
    }

    private fun observeContactBlockingSettings() {
        viewModelScope.launch {
            observeBlockUnknownContactInvites().collect { enabled ->
                _uiState.update {
                    it.copy(blockUnknownContactInvites = enabled)
                }
            }
        }

        viewModelScope.launch {
            observeBlockedContactIds().collect { contactIds ->
                _uiState.update {
                    it.copy(blockedContactCount = contactIds.size)
                }
            }
        }
    }

    private fun changeBlockUnknownContactInvites(enabled: Boolean) {
        viewModelScope.launch {
            setBlockUnknownContactInvites(enabled)
        }
    }

    private fun changeDirectIdentitySetupMode(mode: DirectIdentitySetupMode) {
        viewModelScope.launch {
            setDirectIdentitySetupMode(mode)

            val message =
                when (mode) {
                    DirectIdentitySetupMode.AUTOMATIC_INVITATION ->
                        "Automatic secure contact setup enabled"
                    DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING ->
                        "Manual identity sharing enabled"
                }

            _effects.send(SettingsEffect.ShowSnackbar(message))
        }
    }

    private fun selectLanguage(language: com.cbgm.securechat.core.ui.locale.AppLanguage) {
        viewModelScope.launch {
            setAppLanguageUseCase(language)
            _uiState.update {
                it.copy(
                    currentLanguage = language,
                    showLanguagePicker = false
                )
            }
            _effects.send(
                SettingsEffect.ShowSnackbar(
                    "Language changed to ${language.name}. Restart the app to apply it everywhere."
                )
            )
        }
    }

    private fun handleVersionTap() {
        if (_uiState.value.isDeveloperModeEnabled) return

        val newCount = _uiState.value.developerModeTapCount + 1
        if (newCount >= DEVELOPER_MODE_TAP_THRESHOLD) {
            viewModelScope.launch {
                setDeveloperModeEnabledUseCase(true)
                _uiState.update {
                    it.copy(
                        isDeveloperModeEnabled = true,
                        developerModeTapCount = 0
                    )
                }
                _effects.send(SettingsEffect.ShowSnackbar("Developer mode enabled"))
            }
            return
        }

        _uiState.update { it.copy(developerModeTapCount = newCount) }
        val remaining = DEVELOPER_MODE_TAP_THRESHOLD - newCount
        if (remaining <= 3) {
            viewModelScope.launch {
                _effects.send(
                    SettingsEffect.ShowSnackbar("$remaining more taps to enable developer mode")
                )
            }
        }
    }
}
