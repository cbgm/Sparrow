package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository

/** A blocked contact is never sent a direct invitation. */
class RequireContactInvitableUseCase(
    private val blocklistRepository: ContactBlocklistRepository
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            check(!blocklistRepository.isBlocked(contactId).getOrThrow()) {
                "Blocked contacts cannot be invited"
            }
        }
}
