package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.core.embedding.domain.model.LocalEmbeddingState
import com.cbgm.sparrow.core.embedding.domain.usecase.ObserveLocalEmbeddingStateUseCase
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyState
import com.cbgm.sparrow.feature.safety.domain.usecase.ObserveMessageSafetyStateUseCase
import com.cbgm.sparrow.feature.search.domain.model.SemanticSearchState
import com.cbgm.sparrow.feature.search.domain.usecase.ObserveSemanticSearchStateUseCase
import com.cbgm.sparrow.feature.settings.domain.model.SettingsDomainContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveSettingsDomainContextUseCase(
    private val observeDirectIdentitySetupMode: ObserveDirectIdentitySetupModeUseCase,
    private val observeBlockUnknownContactInvites: ObserveBlockUnknownContactInvitesUseCase,
    private val observeBlockedContactIds: ObserveBlockedContactIdsUseCase,
    private val observeLocalEmbeddingState: ObserveLocalEmbeddingStateUseCase,
    private val observeSemanticSearchState: ObserveSemanticSearchStateUseCase,
    private val observeMessageSafetyState: ObserveMessageSafetyStateUseCase
) {
    operator fun invoke(): Flow<SettingsDomainContext> {
        val privacyContext =
            combine(
                observeDirectIdentitySetupMode(),
                observeBlockUnknownContactInvites(),
                observeBlockedContactIds()
            ) { identitySetupMode, blockUnknownInvites, blockedContactIds ->
                PrivacyContext(
                    identitySetupMode = identitySetupMode,
                    blockUnknownContactInvites = blockUnknownInvites,
                    blockedContactCount = blockedContactIds.size
                )
            }

        val localAiContext =
            combine(
                observeLocalEmbeddingState(),
                observeSemanticSearchState(),
                observeMessageSafetyState()
            ) { localEmbeddingState, semanticSearchState, messageSafetyState ->
                LocalAiContext(
                    localEmbeddingState = localEmbeddingState,
                    semanticSearchState = semanticSearchState,
                    messageSafetyState = messageSafetyState
                )
            }

        return combine(privacyContext, localAiContext) { privacy, localAi ->
            SettingsDomainContext(
                identitySetupMode = privacy.identitySetupMode,
                blockUnknownContactInvites = privacy.blockUnknownContactInvites,
                blockedContactCount = privacy.blockedContactCount,
                localEmbeddingState = localAi.localEmbeddingState,
                semanticSearchState = localAi.semanticSearchState,
                messageSafetyState = localAi.messageSafetyState
            )
        }
    }

    private data class PrivacyContext(
        val identitySetupMode: DirectIdentitySetupMode,
        val blockUnknownContactInvites: Boolean,
        val blockedContactCount: Int
    )

    private data class LocalAiContext(
        val localEmbeddingState: LocalEmbeddingState,
        val semanticSearchState: SemanticSearchState,
        val messageSafetyState: MessageSafetyState
    )
}
