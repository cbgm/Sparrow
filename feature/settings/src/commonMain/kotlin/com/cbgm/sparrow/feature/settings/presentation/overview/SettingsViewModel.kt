package com.cbgm.sparrow.feature.settings.presentation.overview

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.ui.locale.AppLanguage
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.search.domain.usecase.ObserveSemanticSearchStateUseCase
import com.cbgm.sparrow.feature.search.domain.usecase.SetSemanticSearchEnabledUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.GetAppLanguageUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.GetBuildInfoUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.GetDeveloperEnabledUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.ObserveBlockUnknownContactInvitesUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.ObserveBlockedContactIdsUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.ObserveDirectIdentitySetupModeUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.SetAppLanguageUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.SetBlockUnknownContactInvitesUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.SetDeveloperEnabledUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.SetDirectIdentitySetupModeUseCase
import com.cbgm.sparrow.feature.settings.presentation.overview.mapper.toUiState
import com.cbgm.sparrow.feature.settings.presentation.overview.model.DEVELOPER_MODE_TAP_THRESHOLD
import com.cbgm.sparrow.feature.settings.presentation.overview.model.SettingsEffect
import com.cbgm.sparrow.feature.settings.presentation.overview.model.SettingsUiEvent
import com.cbgm.sparrow.feature.settings.presentation.overview.model.SettingsUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val setAppLanguageUseCase: SetAppLanguageUseCase,
    private val getAppLanguageUseCase: GetAppLanguageUseCase,
    private val getDeveloperEnabledUseCase: GetDeveloperEnabledUseCase,
    getBuildInfoUseCase: GetBuildInfoUseCase,
    private val setDeveloperModeEnabledUseCase: SetDeveloperEnabledUseCase,
    observeDirectIdentitySetupMode: ObserveDirectIdentitySetupModeUseCase,
    private val setDirectIdentitySetupMode: SetDirectIdentitySetupModeUseCase,
    observeBlockUnknownContactInvites: ObserveBlockUnknownContactInvitesUseCase,
    private val setBlockUnknownContactInvites: SetBlockUnknownContactInvitesUseCase,
    observeBlockedContactIds: ObserveBlockedContactIdsUseCase,
    observeSemanticSearchState: ObserveSemanticSearchStateUseCase,
    private val setSemanticSearchEnabled: SetSemanticSearchEnabledUseCase
) : BaseViewModel() {
    private val buildInfo = getBuildInfoUseCase()
    private val localState = MutableStateFlow(SettingsLocalState())

    val uiState: StateFlow<SettingsUiState> =
        combine(
            observeDirectIdentitySetupMode(),
            observeBlockUnknownContactInvites(),
            observeBlockedContactIds(),
            observeSemanticSearchState(),
            localState
        ) { identitySetupMode, blockUnknownInvites, blockedContactIds, semanticSearchState, local ->
            buildInfo.toUiState(
                currentLanguage = local.currentLanguage,
                identitySetupMode = identitySetupMode,
                blockUnknownContactInvites = blockUnknownInvites,
                blockedContactCount = blockedContactIds.size,
                semanticSearchState = semanticSearchState,
                isDeveloperModeEnabled = local.isDeveloperModeEnabled,
                developerModeTapCount = local.developerModeTapCount,
                showLanguagePicker = local.showLanguagePicker
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = SettingsUiState(buildInfo = buildInfo)
        )

    private val _effects = Channel<SettingsEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadLocalSettings()
    }

    fun onUiEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.LanguagePickerOpened ->
                localState.update { it.copy(showLanguagePicker = true) }
            SettingsUiEvent.LanguagePickerDismissed ->
                localState.update { it.copy(showLanguagePicker = false) }
            is SettingsUiEvent.LanguageSelected -> selectLanguage(event.language)
            is SettingsUiEvent.DirectIdentitySetupModeChanged -> changeDirectIdentitySetupMode(event.mode)
            is SettingsUiEvent.BlockUnknownContactInvitesChanged -> changeBlockUnknownContactInvites(event.enabled)
            is SettingsUiEvent.SemanticSearchEnabledChanged -> setSemanticSearchEnabled(event.enabled)
            SettingsUiEvent.PrivacyPolicyClicked -> navigator.navigateTo(AppRoute.PrivacyPolicy)
            SettingsUiEvent.DataDisclaimerClicked -> navigator.navigateTo(AppRoute.DataDisclaimer)
            SettingsUiEvent.LicensesClicked -> navigator.navigateTo(AppRoute.Licenses)
            SettingsUiEvent.DeveloperMenuClicked -> navigator.navigateTo(AppRoute.DeveloperMenu)
            SettingsUiEvent.BlockedContactsClicked -> navigator.navigateTo(AppRoute.BlockedContacts)
            SettingsUiEvent.ProfileClicked -> navigator.navigateTo(AppRoute.ProfileSettings)
            SettingsUiEvent.ControlPlanesClicked -> navigator.navigateTo(AppRoute.ControlPlanes)
            SettingsUiEvent.VersionRowTapped -> handleVersionTap()
        }
    }

    private fun loadLocalSettings() {
        viewModelScope.launch {
            localState.update {
                it.copy(
                    currentLanguage = getAppLanguageUseCase(),
                    isDeveloperModeEnabled = getDeveloperEnabledUseCase()
                )
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

    private fun selectLanguage(language: AppLanguage) {
        viewModelScope.launch {
            setAppLanguageUseCase(language)
            localState.update {
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
        val state = localState.value
        if (state.isDeveloperModeEnabled) return

        val newCount = state.developerModeTapCount + 1
        if (newCount >= DEVELOPER_MODE_TAP_THRESHOLD) {
            viewModelScope.launch {
                setDeveloperModeEnabledUseCase(true)
                localState.update {
                    it.copy(
                        isDeveloperModeEnabled = true,
                        developerModeTapCount = 0
                    )
                }
                _effects.send(SettingsEffect.ShowSnackbar("Developer mode enabled"))
            }
            return
        }

        localState.update { it.copy(developerModeTapCount = newCount) }
        val remaining = DEVELOPER_MODE_TAP_THRESHOLD - newCount
        if (remaining <= 3) {
            _effects.trySend(SettingsEffect.ShowSnackbar("$remaining more taps to enable developer mode"))
        }
    }

    private data class SettingsLocalState(
        val currentLanguage: AppLanguage = AppLanguage.ENGLISH,
        val isDeveloperModeEnabled: Boolean = false,
        val developerModeTapCount: Int = 0,
        val showLanguagePicker: Boolean = false
    )
}
