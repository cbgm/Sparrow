package com.cbgm.sparrow.feature.membership.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GroupMembershipLock {
    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T =
        mutex.withLock { block() }
}
