package com.cbgm.sparrow.feature.settings.presentation.overview.mapper

import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.ui.locale.AppLanguage
import com.cbgm.sparrow.feature.search.domain.model.SemanticSearchState
import com.cbgm.sparrow.feature.settings.domain.model.BuildInfo
import com.cbgm.sparrow.feature.settings.presentation.overview.model.SettingsUiState

internal fun BuildInfo.toUiState(
    currentLanguage: AppLanguage,
    identitySetupMode: DirectIdentitySetupMode,
    blockUnknownContactInvites: Boolean,
    blockedContactCount: Int,
    semanticSearchState: SemanticSearchState,
    isDeveloperModeEnabled: Boolean,
    developerModeTapCount: Int,
    showLanguagePicker: Boolean
): SettingsUiState =
    SettingsUiState(
        currentLanguage = currentLanguage,
        directIdentitySetupMode = identitySetupMode,
        blockUnknownContactInvites = blockUnknownContactInvites,
        blockedContactCount = blockedContactCount,
        semanticSearchState = semanticSearchState,
        buildInfo = this,
        isDeveloperModeEnabled = isDeveloperModeEnabled,
        developerModeTapCount = developerModeTapCount,
        showLanguagePicker = showLanguagePicker
    )
