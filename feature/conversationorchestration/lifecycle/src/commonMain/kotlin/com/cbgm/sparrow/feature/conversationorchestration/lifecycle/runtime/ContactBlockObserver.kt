package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime

import com.cbgm.sparrow.core.util.flow.collectUntilHandled
import com.cbgm.sparrow.core.util.flow.retryWithBackoff
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand

/**
 * Watches persisted Contact state; only the internal workflow handler performs cross-feature actions.
 *
 * A contact whose block could not be handled is tried again with backoff, not only when the blocklist changes.
 */
class ContactBlockObserver internal constructor(
    private val blocklistRepository: ContactBlocklistRepository,
    private val engine: WorkflowEngine
) {
    private val logger = SparrowLog.withTag("ContactBlockObserver")

    suspend fun run() {
        var completed: Set<String> = emptySet()
        blocklistRepository.observeBlockedContactIds().retryWithBackoff().collectUntilHandled { blocked ->
            completed = completed.intersect(blocked)
            completed = completed + (blocked - completed).filter { peerId -> handleBlocked(peerId) }
            completed.containsAll(blocked)
        }
    }

    private suspend fun handleBlocked(peerId: String): Boolean =
        engine.run(LifecycleCommand.HandleContactBlocked(peerId))
            .onFailure { error -> logger.error(error) { "Could not revoke exchange for blocked contact $peerId" } }
            .isSuccess
}
