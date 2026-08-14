package com.cbgm.sparrow.feature.chats.data.group.membership

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class GroupMembershipLock {
    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T =
        mutex.withLock { block() }
}
