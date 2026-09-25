package com.cbgm.sparrow.feature.conversationorchestration.runtime

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler

/** Watches persisted Contact state; only the internal workflow handler performs cross-feature actions. */
class ContactBlockObserver internal constructor(
    private val blocklistRepository: ContactBlocklistRepository,
    private val flowHandler: ConversationFlowHandler
) {
    private val logger = SparrowLog.withTag("ContactBlockObserver")

    suspend fun run() {
        var completed: Set<String> = emptySet()
        blocklistRepository.observeBlockedContactIds().collect { blocked ->
            completed = completed.intersect(blocked)
            (blocked - completed).forEach { peerId ->
                flowHandler.onContactBlocked(peerId)
                    .onSuccess { completed = completed + peerId }
                    .onFailure { error ->
                        logger.error(error) { "Could not revoke exchange for blocked contact $peerId" }
                    }
            }
        }
    }
}
