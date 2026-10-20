package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand

/** Watches persisted Contact state; only the internal workflow handler performs cross-feature actions. */
class ContactBlockObserver internal constructor(
    private val blocklistRepository: ContactBlocklistRepository,
    private val engine: WorkflowEngine
) {
    private val logger = SparrowLog.withTag("ContactBlockObserver")

    suspend fun run() {
        var completed: Set<String> = emptySet()
        blocklistRepository.observeBlockedContactIds().collect { blocked ->
            completed = completed.intersect(blocked)
            (blocked - completed).forEach { peerId ->
                engine.run(LifecycleCommand.HandleContactBlocked(peerId))
                    .onSuccess { completed = completed + peerId }
                    .onFailure { error ->
                        logger.error(error) { "Could not revoke exchange for blocked contact $peerId" }
                    }
            }
        }
    }
}
