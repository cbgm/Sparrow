package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.command.MembershipCommand
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationResultsUseCase

class InvitationResultObserver internal constructor(
    private val observeInvitationResults: ObserveInvitationResultsUseCase,
    private val engine: WorkflowEngine
) {
    private val logger = SparrowLog.withTag("InvitationResultObserver")

    suspend fun run() {
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
                            engine.run(MembershipCommand.RecoverAcceptedGroupInvitation(result))
                        result.payloadType == InvitationPayloadType.DIRECT ->
                            engine.run(LifecycleCommand.RecoverAcceptedDirectInvitation(result))
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
                handle(result)
                    .onFailure { error ->
                        logger.error(error) {
                            "Invitation result workflow failed for invitationId=${result.invitationId}"
                        }
                    }
            }
        }
    }

    private suspend fun handle(result: InvitationResult) =
        when (result.payloadType) {
            InvitationPayloadType.DIRECT ->
                engine.run(LifecycleCommand.HandleDirectInvitationResult(result))
            InvitationPayloadType.GROUP -> engine.run(MembershipCommand.HandleGroupInvitationResult(result))
        }

    private fun InvitationResult.eventKey(): String =
        "$invitationId:${direction.name}:${response.name}:${action?.name.orEmpty()}"
}
