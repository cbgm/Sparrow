package com.cbgm.sparrow.feature.conversationorchestration.runtime

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResult
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResultStatus
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveIdentityResultsUseCase

class IdentityResultObserver internal constructor(
    private val observeResults: ObserveIdentityResultsUseCase,
    private val flowHandler: ConversationFlowHandler
) {
    private val logger = SparrowLog.withTag("IdentityResultObserver")

    suspend fun run() {
        val seen = mutableSetOf<String>()
        var initialized = false

        observeResults().collect { results ->
            if (!initialized) {
                results.forEach { result ->
                    seen += result.eventKey()
                    if (result.status == IdentityResultStatus.INCOMING_EXCHANGE ||
                        result.status == IdentityResultStatus.EXCHANGE_INVALIDATED
                    ) {
                        forward(result)
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
