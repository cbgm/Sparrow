package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime

import com.cbgm.sparrow.core.util.flow.collectUntilHandled
import com.cbgm.sparrow.core.util.flow.retryWithBackoff
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResult
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResultStatus
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveIdentityResultsUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObservePendingRemoteIdentityChangesUseCase
import kotlinx.coroutines.flow.first

/**
 * Forwards identity results to the lifecycle workflows for the app's lifetime. A result counts as seen only
 * once its workflow succeeded; a failed one is tried again with backoff.
 */
class IdentityResultObserver internal constructor(
    private val observeResults: ObserveIdentityResultsUseCase,
    private val engine: WorkflowEngine,
    private val observePendingRemoteIdentityChanges: ObservePendingRemoteIdentityChangesUseCase
) {
    private val logger = SparrowLog.withTag("IdentityResultObserver")

    suspend fun run() {
        val seen = mutableSetOf<String>()
        // Decided once for the results present at start; every later result is forwarded.
        var startActions: Map<String, StartAction>? = null

        observeResults().retryWithBackoff().collectUntilHandled { results ->
            val actions = startActions ?: findStartActions(results)?.also { startActions = it }
            if (actions == null) return@collectUntilHandled false
            results.map { result -> handleUnseen(result, actions, seen) }.all { handled -> handled }
        }
    }

    private suspend fun handleUnseen(
        result: IdentityResult,
        startActions: Map<String, StartAction>,
        seen: MutableSet<String>
    ): Boolean {
        val key = result.eventKey()
        if (key in seen) return true
        val handled = handle(result, startActions[key])
        if (handled) seen += key
        return handled
    }

    /**
     * A process can die after a direct exchange becomes MUTUAL but before its WAITING_FOR_AUTHORIZATION
     * messages are released. Only reconcile established exchanges. Only an exact still-active exchange can
     * create a chat missing due to a crash during acceptance; a closed exchange cannot resurrect a deliberately
     * deleted chat. Returns null when the pending identity changes could not be read; the start is tried again.
     */
    private suspend fun findStartActions(results: List<IdentityResult>): Map<String, StartAction>? {
        val pendingReplacementPeers =
            safeSuspendCall(logFailure = false) { observePendingRemoteIdentityChanges().first() }
                .onFailure { error -> logger.warn(error) { "Could not read pending identity changes" } }
                .getOrNull()
                ?.mapTo(mutableSetOf()) { change -> change.peerId }
                ?: return null
        val reconciledPeers = mutableSetOf<String>()
        return results.associate { result ->
            result.eventKey() to startAction(result, pendingReplacementPeers, reconciledPeers)
        }
    }

    private fun startAction(
        result: IdentityResult,
        pendingReplacementPeers: Set<String>,
        reconciledPeers: MutableSet<String>
    ): StartAction =
        when (result.status) {
            IdentityResultStatus.INCOMING_EXCHANGE,
            IdentityResultStatus.EXCHANGE_INVALIDATED -> StartAction.FORWARD
            IdentityResultStatus.ESTABLISHED -> reconcileAction(result, pendingReplacementPeers, reconciledPeers)
            else -> StartAction.NONE
        }

    private fun reconcileAction(
        result: IdentityResult,
        pendingReplacementPeers: Set<String>,
        reconciledPeers: MutableSet<String>
    ): StartAction =
        if (result.peerId !in pendingReplacementPeers && reconciledPeers.add(result.peerId)) {
            StartAction.RECONCILE
        } else {
            StartAction.NONE
        }

    /** [startAction] is null for a result that arrived after the start. */
    private suspend fun handle(result: IdentityResult, startAction: StartAction?): Boolean =
        when (startAction) {
            null, StartAction.FORWARD -> forward(result)
            StartAction.RECONCILE -> reconcile(result)
            StartAction.NONE -> true
        }

    private suspend fun reconcile(result: IdentityResult): Boolean =
        engine.run(LifecycleCommand.RecoverAuthorizedConversation(result))
            .onFailure { error ->
                logger.warn(error) { "Could not reconcile pending direct messages for ${result.peerId}" }
            }
            .isSuccess

    private suspend fun forward(result: IdentityResult): Boolean =
        engine
            .run(LifecycleCommand.HandleIdentityResult(result))
            .onFailure { error ->
                logger.warn(error) {
                    "Identity workflow failed for exchangeId=${result.exchangeId}, status=${result.status}"
                }
            }
            .isSuccess

    private fun IdentityResult.eventKey(): String =
        "$exchangeId:${status.name}:$updatedAtEpochMilliseconds"

    private enum class StartAction {
        FORWARD,
        RECONCILE,
        NONE
    }
}
