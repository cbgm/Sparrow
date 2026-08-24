package com.cbgm.sparrow.startup.util

import com.cbgm.sparrow.core.embedding.domain.usecase.InitializeLocalEmbeddingUseCase
import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityStatusUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.RecoverIncompleteIdentityUseCase
import com.cbgm.sparrow.feature.safety.domain.usecase.InitializeMessageSafetyUseCase
import com.cbgm.sparrow.feature.search.domain.usecase.InitializeSemanticSearchUseCase
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionManager
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionState
import com.cbgm.sparrow.startup.presentation.start.model.AppInitializationResult
import kotlinx.coroutines.flow.first

class AppInitializer(
    private val getIdentityStatus: GetIdentityStatusUseCase,
    private val recoverIncompleteIdentity: RecoverIncompleteIdentityUseCase,
    private val initializeLocalEmbedding: InitializeLocalEmbeddingUseCase,
    private val initializeSemanticSearch: InitializeSemanticSearchUseCase,
    private val initializeMessageSafety: InitializeMessageSafetyUseCase,
    private val transportConnectionManager: TransportConnectionManager
) {
    suspend fun initialize(): Result<AppInitializationResult> =
        runCatching {
            val identityStatus = resolveIdentityStatus()
            if (identityStatus != IdentityStatus.READY) {
                return@runCatching AppInitializationResult.IdentityRequired
            }

            initializeLocalEmbedding()
            initializeSemanticSearch()
            initializeMessageSafety()

            when (awaitInitialConnectionResult()) {
                is TransportConnectionState.Connected -> AppInitializationResult.ReadyOnline
                is TransportConnectionState.Failed -> AppInitializationResult.ReadyOffline
                else -> error("Initial transport result must be connected or failed")
            }
        }

    private suspend fun resolveIdentityStatus(): IdentityStatus {
        val initialStatus = getIdentityStatus().getOrThrow()
        if (initialStatus != IdentityStatus.INCOMPLETE) return initialStatus
        recoverIncompleteIdentity().getOrThrow()
        return getIdentityStatus().getOrThrow()
    }

    private suspend fun awaitInitialConnectionResult(): TransportConnectionState =
        transportConnectionManager.connectionState.first { state ->
            state is TransportConnectionState.Connected || state is TransportConnectionState.Failed
        }
}
