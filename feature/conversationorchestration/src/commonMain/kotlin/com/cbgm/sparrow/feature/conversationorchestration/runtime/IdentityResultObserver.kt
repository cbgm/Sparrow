package com.cbgm.sparrow.feature.conversationorchestration.runtime

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResult
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResultStatus
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveIdentityResultsUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObservePendingRemoteIdentityChangesUseCase
import kotlinx.coroutines.flow.first

class IdentityResultObserver internal constructor(
    private val observeResults: ObserveIdentityResultsUseCase,
    private val flowHandler: ConversationFlowHandler,
    private val observePendingRemoteIdentityChanges: ObservePendingRemoteIdentityChangesUseCase
) {
    private val logger = SparrowLog.withTag("IdentityResultObserver")

    suspend fun run() {
        val seen = mutableSetOf<String>()
        var initialized = false

        observeResults().collect { results ->
            if (!initialized) {
                // A process can die after a direct exchange becomes MUTUAL but
                // before its WAITING_FOR_AUTHORIZATION messages are released. Only
                // reconcile existing conversations: do NOT replay historical
                // invitation acceptance or recreate chats deliberately deleted.
                val pendingReplacementPeers = observePendingRemoteIdentityChanges()
                    .first().mapTo(mutableSetOf()) { it.peerId }
                val reconciledPeers = mutableSetOf<String>()
                results.forEach { result ->
                    seen += result.eventKey()
                    when (result.status) {
                        IdentityResultStatus.INCOMING_EXCHANGE,
                        IdentityResultStatus.EXCHANGE_INVALIDATED -> forward(result)
                        IdentityResultStatus.ESTABLISHED -> {
                            if (result.peerId !in pendingReplacementPeers &&
                                reconciledPeers.add(result.peerId)
                            ) {
                                flowHandler.recoverExistingAuthorizedConversation(result.peerId)
                                    .onFailure { error ->
                                        logger.warn(error) {
                                            "Could not reconcile pending direct messages for ${result.peerId}"
                                        }
                                    }
                            }
                        }
                        else -> Unit
                    }
                }
                initialized = true
                return@collect
            }

            results.forEach { result ->
                if (!seen.add(result.eventKey())) return@forEach
                forward(result)
            }
        }
    }

    private suspend fun forward(result: IdentityResult) {
        flowHandler
            .onIdentityResult(result)
            .onFailure { error ->
                logger.warn(error) {
                    "Identity workflow failed for exchangeId=${result.exchangeId}, status=${result.status}"
                }
            }
    }

    private fun IdentityResult.eventKey(): String =
        "$exchangeId:${status.name}:$updatedAtEpochMilliseconds"
}
