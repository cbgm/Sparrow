package com.cbgm.sparrow.feature.conversationorchestration.runtime

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler
import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentityResult
import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentityResultType
import com.cbgm.sparrow.feature.identity.domain.usecase.direct.ObserveDirectIdentityResultsUseCase

class DirectIdentityResultObserver internal constructor(
    private val observeResults: ObserveDirectIdentityResultsUseCase,
    private val flowHandler: ConversationFlowHandler
) {
    private val logger = SparrowLog.withTag("DirectIdentityResultObserver")

    suspend fun run() {
        val seen = mutableSetOf<String>()
        var initialized = false

        observeResults().collect { results ->
            if (!initialized) {
                results.forEach { result ->
                    seen += result.eventKey()
                    if (result.type == DirectIdentityResultType.INCOMING_INVITATION) {
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

    private suspend fun forward(result: DirectIdentityResult) {
        flowHandler
            .onDirectIdentityResult(result)
            .onFailure { error ->
                logger.warn(error) {
                    "Direct identity workflow failed for invitationId=${result.invitationId}, type=${result.type}"
                }
            }
    }

    private fun DirectIdentityResult.eventKey(): String =
        "$invitationId:${type.name}:$updatedAtEpochMilliseconds"
}
