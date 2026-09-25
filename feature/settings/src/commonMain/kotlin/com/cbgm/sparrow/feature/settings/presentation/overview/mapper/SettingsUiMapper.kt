package com.cbgm.sparrow.feature.settings.presentation.overview.mapper

import com.cbgm.sparrow.core.embedding.domain.model.LocalEmbeddingState
import com.cbgm.sparrow.core.ui.locale.AppLanguage
import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyState
import com.cbgm.sparrow.feature.search.domain.model.SemanticSearchState
import com.cbgm.sparrow.feature.settings.domain.model.BuildInfo
import com.cbgm.sparrow.feature.settings.presentation.overview.model.SettingsUiState

internal fun BuildInfo.toSettingsUiState(
    currentLanguage: AppLanguage,
    activeAutoReplyName: String?,
    identitySetupMode: DirectIdentitySetupMode,
    blockUnknownContactInvites: Boolean,
    blockedContactCount: Int,
    localEmbeddingState: LocalEmbeddingState,
    semanticSearchState: SemanticSearchState,
    messageSafetyState: MessageSafetyState,
    voiceTranscriptionEnabled: Boolean,
    isDeveloperModeEnabled: Boolean,
    developerModeTapCount: Int,
    showLanguagePicker: Boolean
): SettingsUiState =
    SettingsUiState(
        currentLanguage = currentLanguage,
        activeAutoReplyName = activeAutoReplyName,
        directIdentitySetupMode = identitySetupMode,
        blockUnknownContactInvites = blockUnknownContactInvites,
        blockedContactCount = blockedContactCount,
        localEmbeddingState = localEmbeddingState,
        semanticSearchState = semanticSearchState,
        messageSafetyState = messageSafetyState,
        voiceTranscriptionEnabled = voiceTranscriptionEnabled,
        buildInfo = this,
        isDeveloperModeEnabled = isDeveloperModeEnabled,
        developerModeTapCount = developerModeTapCount,
        showLanguagePicker = showLanguagePicker
    )
