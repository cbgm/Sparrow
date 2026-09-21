package com.cbgm.sparrow.feature.conversationorchestration.runtime

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler
import com.cbgm.sparrow.feature.membership.domain.model.MembershipPerspective
import com.cbgm.sparrow.feature.membership.domain.model.MembershipResult
import com.cbgm.sparrow.feature.membership.domain.model.MembershipStatus
import com.cbgm.sparrow.feature.membership.domain.usecase.ObserveMembershipResultsUseCase

class MembershipResultObserver internal constructor(
    private val observeMembershipResults: ObserveMembershipResultsUseCase,
    private val flowHandler: ConversationFlowHandler
) {
    private val logger = SparrowLog.withTag("MembershipResultObserver")

    suspend fun run() {
        val seen = mutableSetOf<String>()
        var initialized = false

        observeMembershipResults().collect { results ->
            if (!initialized) {
                results.forEach { result ->
                    seen += result.eventKey()
                    if (
                        result.perspective == MembershipPerspective.OWNER &&
                        result.status in setOf(
                            MembershipStatus.IDENTITY_READY,
                            MembershipStatus.WELCOME_SENT,
                            MembershipStatus.ACTIVE
                        )
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

    private suspend fun forward(result: MembershipResult) {
        flowHandler
            .onMembershipResult(result)
            .onFailure { error ->
                logger.error(error) {
                    "Membership result workflow failed for membershipId=${result.membershipId}"
                }
            }
    }

    private fun MembershipResult.eventKey(): String =
        "$membershipId:${status.name}:$updatedAtEpochMilliseconds"
}
