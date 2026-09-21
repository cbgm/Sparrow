package com.cbgm.sparrow.feature.conversationorchestration.runtime

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.ConversationPort
import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationResultsUseCase
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class InvitationResultObserver internal constructor(
    private val observeInvitationResults: ObserveInvitationResultsUseCase,
    private val flowHandler: ConversationFlowHandler,
    private val conversationPort: ConversationPort
) {
    private val logger = SparrowLog.withTag("InvitationResultObserver")

    suspend fun run(): Unit =
        coroutineScope {
            launch { observeResults() }
            launch { conversationPort.runPendingAuthorizationCleanup() }
        }

    private suspend fun observeResults() {
        val seen = mutableSetOf<String>()
        var initialized = false

        observeInvitationResults().collect { results ->
            if (!initialized) {
                // A receiver may have accepted before the observer was started.
                // Replay only still-active incoming direct acceptances. The workflow
                // checks that the exact exchange was not later closed/revoked.
                results.forEach { result ->
                    seen += result.eventKey()
                    val recovery = when {
                        result.payloadType == InvitationPayloadType.GROUP &&
                            result.direction == InvitationDirection.INCOMING &&
                            result.response == InvitationResponse.ACCEPTED ->
                            flowHandler.recoverAcceptedGroupInvitation(result)
                        result.payloadType == InvitationPayloadType.DIRECT ->
                            flowHandler.recoverAcceptedDirectInvitation(result)
                        else -> null
                    }
                    recovery?.onFailure { error ->
                        logger.error(error) {
                            "Could not recover accepted invitation ${result.invitationId}"
                        }
                    }
                }
                initialized = true
                return@collect
            }

            results.forEach { result ->
                if (!seen.add(result.eventKey())) return@forEach
                flowHandler
                    .onInvitationResult(result)
                    .onFailure { error ->
                        logger.error(error) {
                            "Invitation result workflow failed for invitationId=${result.invitationId}"
                        }
                    }
            }
        }
    }

    private fun InvitationResult.eventKey(): String =
        "$invitationId:${direction.name}:${response.name}:${action?.name.orEmpty()}"
}
