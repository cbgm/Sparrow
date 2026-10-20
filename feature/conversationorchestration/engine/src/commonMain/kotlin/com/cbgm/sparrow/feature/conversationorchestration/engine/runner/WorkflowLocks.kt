package com.cbgm.sparrow.feature.conversationorchestration.engine.runner

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes workflows that share a lock key; workflows without a key run unrestricted. */
class WorkflowLocks {
    private val registryMutex = Mutex()
    private val locks = mutableMapOf<String, Mutex>()

    suspend fun <T> withLock(key: String?, action: suspend () -> T): T {
        if (key == null) return action()
        return lockFor(key).withLock { action() }
    }

    private suspend fun lockFor(key: String): Mutex =
        registryMutex.withLock { locks.getOrPut(key) { Mutex() } }
}
