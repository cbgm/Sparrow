package com.cbgm.sparrow.server.gateway

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

class BlobCleanupAgent(
    private val store: BlobStore,
    private val cleanupIntervalMilliseconds: Long,
    private val now: () -> Long = System::currentTimeMillis
) {
    init {
        require(cleanupIntervalMilliseconds > 0L)
    }

    suspend fun run() {
        while (currentCoroutineContext().isActive) {
            store.purgeExpired(now())
            delay(cleanupIntervalMilliseconds.milliseconds)
        }
    }
}
