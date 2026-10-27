package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime

import com.cbgm.sparrow.core.util.flow.collectUntilHandled
import com.cbgm.sparrow.core.util.flow.retryWithBackoff
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.command.MembershipCommand
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationResultsUseCase

/**
 * Forwards invitation results to the lifecycle and membership workflows for the app's lifetime. A result counts
 * as seen only once its workflow succeeded; a failed one is tried again with backoff.
 */
class InvitationResultObserver internal constructor(
    private val observeInvitationResults: ObserveInvitationResultsUseCase,
    private val engine: WorkflowEngine
) {
    private val logger = SparrowLog.withTag("InvitationResultObserver")

    suspend fun run() {
        val seen = mutableSetOf<String>()
        // Results present at start are recovered instead of handled; every later result is handled.
        var startKeys: Set<String>? = null

        observeInvitationResults().retryWithBackoff().collectUntilHandled { results ->
            val atStart = startKeys ?: results.mapTo(mutableSetOf()) { result -> result.eventKey() }
            startKeys = atStart
            results.map { result -> handleUnseen(result, result.eventKey() in atStart, seen) }.all { it }
        }
    }

    private suspend fun handleUnseen(
        result: InvitationResult,
        presentAtStart: Boolean,
        seen: MutableSet<String>
    ): Boolean {
        val key = result.eventKey()
        if (key in seen) return true
        val handled = if (presentAtStart) recover(result) else handle(result)
        if (handled) seen += key
        return handled
    }

    /**
     * A receiver may have accepted before the observer was started. Replay only still-active incoming direct
     * acceptances. The workflow checks that the exact exchange was not later closed/revoked.
     */
    private suspend fun recover(result: InvitationResult): Boolean {
        val recovery = when {
            result.payloadType == InvitationPayloadType.GROUP &&
                result.direction == InvitationDirection.INCOMING &&
                result.response == InvitationResponse.ACCEPTED ->
                engine.run(MembershipCommand.RecoverAcceptedGroupInvitation(result))
            result.payloadType == InvitationPayloadType.DIRECT ->
                engine.run(LifecycleCommand.RecoverAcceptedDirectInvitation(result))
            else -> return true
        }
        return recovery
            .onFailure { error ->
                logger.error(error) { "Could not recover accepted invitation ${result.invitationId}" }
            }
            .isSuccess
    }

    private suspend fun handle(result: InvitationResult): Boolean =
        startWorkflow(result)
            .onFailure { error ->
                logger.error(error) { "Invitation result workflow failed for invitationId=${result.invitationId}" }
            }
            .isSuccess

    private suspend fun startWorkflow(result: InvitationResult): Result<ConversationResult> =
        when (result.payloadType) {
            InvitationPayloadType.DIRECT ->
                engine.run(LifecycleCommand.HandleDirectInvitationResult(result))
            InvitationPayloadType.GROUP -> engine.run(MembershipCommand.HandleGroupInvitationResult(result))
        }

    private fun InvitationResult.eventKey(): String =
        "$invitationId:${direction.name}:${response.name}:${action?.name.orEmpty()}"
}
