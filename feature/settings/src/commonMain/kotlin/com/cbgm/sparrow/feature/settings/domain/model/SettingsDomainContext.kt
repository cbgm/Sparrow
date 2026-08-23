package com.cbgm.sparrow.feature.settings.domain.model

import com.cbgm.sparrow.core.embedding.domain.model.LocalEmbeddingState
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyState
import com.cbgm.sparrow.feature.search.domain.model.SemanticSearchState

data class SettingsDomainContext(
    val identitySetupMode: DirectIdentitySetupMode,
    val blockUnknownContactInvites: Boolean,
    val blockedContactCount: Int,
    val localEmbeddingState: LocalEmbeddingState,
    val semanticSearchState: SemanticSearchState,
    val messageSafetyState: MessageSafetyState
)
