package com.cbgm.sparrow.feature.conversationorchestration.membership.runtime

import com.cbgm.sparrow.core.util.flow.collectUntilHandled
import com.cbgm.sparrow.core.util.flow.retryWithBackoff
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.command.MembershipCommand
import com.cbgm.sparrow.feature.membership.domain.model.MembershipPerspective
import com.cbgm.sparrow.feature.membership.domain.model.MembershipResult
import com.cbgm.sparrow.feature.membership.domain.model.MembershipStatus
import com.cbgm.sparrow.feature.membership.domain.usecase.ObserveMembershipResultsUseCase

/**
 * Forwards membership results to the membership workflow for the app's lifetime. A result counts as seen only
 * once its workflow succeeded; a failed one is tried again with backoff.
 */
class MembershipResultObserver internal constructor(
    private val observeMembershipResults: ObserveMembershipResultsUseCase,
    private val engine: WorkflowEngine
) {
    private val logger = SparrowLog.withTag("MembershipResultObserver")

    suspend fun run() {
        val seen = mutableSetOf<String>()
        // Results present at start are only resumed when the owner still has work to do.
        var startKeys: Set<String>? = null

        observeMembershipResults().retryWithBackoff().collectUntilHandled { results ->
            val atStart = startKeys ?: results.mapTo(mutableSetOf()) { result -> result.eventKey() }
            startKeys = atStart
            results.map { result -> handleUnseen(result, result.eventKey() in atStart, seen) }.all { it }
        }
    }

    private suspend fun handleUnseen(
        result: MembershipResult,
        presentAtStart: Boolean,
        seen: MutableSet<String>
    ): Boolean {
        val key = result.eventKey()
        if (key in seen) return true
        val handled = if (presentAtStart && !result.isResumedAtStart()) true else forward(result)
        if (handled) seen += key
        return handled
    }

    private fun MembershipResult.isResumedAtStart(): Boolean =
        perspective == MembershipPerspective.OWNER && status in RESUMED_AT_START

    private suspend fun forward(result: MembershipResult): Boolean =
        engine
            .run(MembershipCommand.HandleMembershipResult(result))
            .onFailure { error ->
                logger.error(error) {
                    "Membership result workflow failed for membershipId=${result.membershipId}"
                }
            }
            .isSuccess

    private fun MembershipResult.eventKey(): String =
        "$membershipId:${status.name}:$updatedAtEpochMilliseconds"

    private companion object {
        val RESUMED_AT_START =
            setOf(MembershipStatus.IDENTITY_READY, MembershipStatus.WELCOME_SENT, MembershipStatus.ACTIVE)
    }
}
