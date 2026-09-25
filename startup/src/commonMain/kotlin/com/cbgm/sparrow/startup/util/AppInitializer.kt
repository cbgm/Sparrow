package com.cbgm.sparrow.startup.util

import com.cbgm.sparrow.core.coroutines.ApplicationCoroutineScope
import com.cbgm.sparrow.core.embedding.domain.usecase.InitializeLocalEmbeddingUseCase
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.logging.StartupTrace
import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityStatusUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.RecoverIncompleteIdentityUseCase
import com.cbgm.sparrow.feature.safety.domain.usecase.InitializeMessageSafetyUseCase
import com.cbgm.sparrow.feature.search.domain.usecase.InitializeSemanticSearchUseCase
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionManager
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionState
import com.cbgm.sparrow.startup.presentation.start.model.AppInitializationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AppInitializer(
    private val getIdentityStatus: GetIdentityStatusUseCase,
    private val recoverIncompleteIdentity: RecoverIncompleteIdentityUseCase,
    private val initializeLocalEmbedding: InitializeLocalEmbeddingUseCase,
    private val initializeSemanticSearch: InitializeSemanticSearchUseCase,
    private val initializeMessageSafety: InitializeMessageSafetyUseCase,
    private val transportConnectionManager: TransportConnectionManager,
    private val runtimeReadiness: StartupRuntimeReadiness,
    private val applicationScope: ApplicationCoroutineScope
) {
    private val logger = SparrowLog.withTag("AppInitializer")
    private var localIntelligenceInitializationJob: Job? = null

    suspend fun initialize(): Result<AppInitializationResult> =
        runCatching {
            StartupTrace.event("Startup initializer started")
            val identityStatus = StartupTrace.measure("resolve local identity status") {
                resolveIdentityStatus()
            }
            StartupTrace.event("identity status=$identityStatus")
            if (identityStatus != IdentityStatus.READY) {
                return@runCatching AppInitializationResult.IdentityRequired
            }

            // Still starts in the application scope; do not await local AI here.
            StartupTrace.event("launching background local intelligence initialization")
            startLocalIntelligenceInitialization()
            // Do not navigate before AppViewModel has finished the mandatory local
            // runtime setup (crypto, saved control-plane config, notification runtime,
            // and result observers). Network connectivity is NOT a startup prerequisite.
            StartupTrace.measure("await required local application runtime") {
                runtimeReadiness.awaitReady()
            }
            val connection = transportConnectionManager.connectionState.value
            StartupTrace.event(
                "local runtime and identity ready; current transport=" +
                    if (connection is TransportConnectionState.Connected) "Connected" else "NotConnected"
            )
            if (connection is TransportConnectionState.Connected) {
                AppInitializationResult.ReadyOnline
            } else {
                // Temporary connecting/disconnected states are handled by the
                // existing foreground runtime and its live connection observer.
                AppInitializationResult.ReadyOffline
            }
        }

    private fun startLocalIntelligenceInitialization() {
        if (localIntelligenceInitializationJob != null) return
        localIntelligenceInitializationJob = applicationScope.launch {
            try {
                // Order is intentional: feature initializers observe the restored
                // embedding settings/state and must not run before they are loaded.
                StartupTrace.measure("background local embedding") { initializeLocalEmbedding() }
                StartupTrace.measure("background semantic search") { initializeSemanticSearch() }
                StartupTrace.measure("background message safety") { initializeMessageSafety() }
                StartupTrace.event("background local intelligence complete")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                logger.error(error) { "Local intelligence initialization failed" }
            }
        }
    }

    private suspend fun resolveIdentityStatus(): IdentityStatus {
        val initialStatus = getIdentityStatus().getOrThrow()
        if (initialStatus != IdentityStatus.INCOMPLETE) return initialStatus
        recoverIncompleteIdentity().getOrThrow()
        return getIdentityStatus().getOrThrow()
    }
}
