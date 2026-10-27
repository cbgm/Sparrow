package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.base.messagepart.domain.model.PollPolicy
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.membership.domain.usecase.ObserveGroupAdministrationUseCase
import kotlinx.coroutines.flow.first

/**
 * Whether a member may close the poll in [pollMessage]: only its creator or a group admin may.
 * A null [closerContactId] is the local user, otherwise it is the remote member who closed the poll.
 */
class CheckPollClosePermissionUseCase(
    private val observeGroupAdministration: ObserveGroupAdministrationUseCase
) {
    suspend operator fun invoke(
        groupId: String,
        pollMessage: StoredMessage,
        closerContactId: String?
    ): Result<Boolean> =
        safeSuspendCall(logFailure = false) {
            val administration = observeGroupAdministration(groupId).first()
            if (closerContactId == null) {
                PollPolicy.canClose(isCreator = pollMessage.isMine, isAdmin = administration.isLocalAdmin)
            } else {
                PollPolicy.canClose(
                    isCreator = pollMessage.senderContactId == closerContactId,
                    isAdmin = closerContactId in administration.adminContactIds
                )
            }
        }
}
